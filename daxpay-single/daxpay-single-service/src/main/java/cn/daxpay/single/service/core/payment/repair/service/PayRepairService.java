package cn.daxpay.single.service.core.payment.repair.service;

import cn.daxpay.single.core.code.PayStatusEnum;
import cn.daxpay.single.core.exception.SystemUnknownErrorException;
import cn.daxpay.single.service.code.PayAdjustWayEnum;
import cn.daxpay.single.service.common.local.PaymentContextLocal;
import cn.daxpay.single.service.core.order.pay.entity.PayOrder;
import cn.daxpay.single.service.core.order.pay.service.PayOrderService;
import cn.daxpay.single.service.core.payment.notice.service.ClientNoticeService;
import cn.daxpay.single.service.core.payment.repair.result.PayRepairResult;
import cn.daxpay.single.service.core.record.flow.service.TradeFlowRecordService;
import cn.daxpay.single.service.core.record.repair.service.PayRepairRecordService;
import cn.daxpay.single.service.func.AbsPayAdjustStrategy;
import cn.daxpay.single.service.util.PayStrategyFactory;
import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 支付修复服务
 * @author xxm
 * @since 2023/12/27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayRepairService {

    private final PayOrderService payOrderService;

    private final ClientNoticeService clientNoticeService;

    private final PayRepairRecordService recordService;

    private final LockTemplate lockTemplate;
    private final TradeFlowRecordService tradeFlowRecordService;

    /**
     * 修复支付单
     */
    @Transactional(rollbackFor = Exception.class)
    public PayRepairResult repair(PayOrder order, PayAdjustWayEnum repairType){
        // 添加分布式锁
        LockInfo lock = lockTemplate.lock("repair:pay:" + order.getId(), 10000, 200);
        if (Objects.isNull(lock)){
            log.warn("当前支付定单正在修复中: {}", order.getId());
            return new PayRepairResult();
        }

        // 2.1 初始化修复参数
        AbsPayAdjustStrategy repairStrategy = PayStrategyFactory.create(order.getChannel(), AbsPayAdjustStrategy.class);
        repairStrategy.setOrder(order);
        // 2.2 执行前置处理
        repairStrategy.doBeforeHandler();

        // 3. 根据不同的类型执行对应的修复逻辑
        switch (repairType) {
            case SUCCESS:
                this.success(order);
                break;
            case CLOSE_LOCAL:
                this.closeLocal(order);
                break;
            case CLOSE_GATEWAY:
                this.closeRemote(order, repairStrategy);
                break;
            default:
                log.error("走到了理论上讲不会走到的分支");
                throw new SystemUnknownErrorException("走到了理论上讲不会走到的分支");
        }
        // 设置修复iD
        // 发送通知
        clientNoticeService.registerPayNotice(order);
        return null;
    }


    /**
     * 变更为已支付
     * 同步: 将异步支付状态修改为成功
     * 回调: 将异步支付状态修改为成功
     */
    private void success(PayOrder order) {
        // 读取支付网关中的完成时间
        LocalDateTime payTime = PaymentContextLocal.get()
                .getRepairInfo()
                .getFinishTime();
        // 修改订单支付状态为成功
        order.setStatus(PayStatusEnum.SUCCESS.getCode())
                .setPayTime(payTime)
                .setCloseTime(null);
        tradeFlowRecordService.savePay(order);
        payOrderService.updateById(order);
    }

    /**
     * 关闭支付
     * 同步/对账: 执行支付单所有的支付通道关闭支付逻辑, 不需要调用网关关闭,
     */
    private void closeLocal(PayOrder order) {
        // 执行策略的关闭方法
        order.setStatus(PayStatusEnum.CLOSE.getCode())
                // TODO 尝试是否可以使用网关返回的时间
                .setCloseTime(LocalDateTime.now());
        payOrderService.updateById(order);
    }
    /**
     * 关闭网关交易, 同时也会关闭本地支付
     * 回调: 执行所有的支付通道关闭支付逻辑
     */
    private void closeRemote(PayOrder payOrder, AbsPayAdjustStrategy strategy) {
        // 执行策略的关闭方法
        strategy.doCloseRemoteHandler();
        payOrder.setStatus(PayStatusEnum.CLOSE.getCode())
                // TODO 尝试是否可以使用网关返回的时间
                .setCloseTime(LocalDateTime.now());
        payOrderService.updateById(payOrder);
    }
}
