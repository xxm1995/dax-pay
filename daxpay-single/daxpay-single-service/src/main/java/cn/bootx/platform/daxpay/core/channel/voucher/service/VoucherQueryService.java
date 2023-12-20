package cn.bootx.platform.daxpay.core.channel.voucher.service;

import cn.bootx.platform.common.core.exception.DataNotExistException;
import cn.bootx.platform.common.core.rest.PageResult;
import cn.bootx.platform.common.core.rest.param.PageParam;
import cn.bootx.platform.common.mybatisplus.util.MpUtil;
import cn.bootx.platform.daxpay.core.channel.voucher.dao.VoucherManager;
import cn.bootx.platform.daxpay.core.channel.voucher.entity.Voucher;
import cn.bootx.platform.daxpay.dto.channel.voucher.VoucherDto;
import cn.bootx.platform.daxpay.exception.pay.PayFailureException;
import cn.bootx.platform.daxpay.param.channel.voucher.VoucherParam;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 储值卡查询
 *
 * @author xxm
 * @since 2022/3/14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherQueryService {

    private final VoucherManager voucherManager;

    /**
     * 分页
     */
    public PageResult<VoucherDto> page(PageParam pageParam, VoucherParam param) {
        return MpUtil.convert2DtoPageResult(voucherManager.page(pageParam, param));
    }

    /**
     * 根据id查询
     */
    public VoucherDto findById(Long id) {
        return voucherManager.findById(id).map(Voucher::toDto).orElseThrow(() -> new DataNotExistException("储值卡不存在"));
    }

    /**
     * 根据卡号查询
     */
    public VoucherDto findByCardNo(String cardNo) {
        return voucherManager.findByCardNo(cardNo)
                .map(Voucher::toDto)
                .orElseThrow(() -> new DataNotExistException("储值卡不存在"));
    }

    /**
     * 获取并判断卡状态
     */
    public VoucherDto getAndJudgeVoucher(String cardNo){
        Voucher voucher = voucherManager.findByCardNo(cardNo)
                .orElseThrow(() -> new DataNotExistException("储值卡不存在"));
        // 过期
        String checkMsg = check(voucher);
        if (StrUtil.isNotBlank(checkMsg)){
            throw new PayFailureException(checkMsg);
        }
        return voucher.toDto();
    }

    /**
     * 卡信息检查
     */
    public String check(Voucher voucher) {
        return null;
    }

}
