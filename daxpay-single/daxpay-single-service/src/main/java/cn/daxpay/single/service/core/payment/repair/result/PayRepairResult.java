package cn.daxpay.single.service.core.payment.repair.result;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 支付修复结果
 * @author xxm
 * @since 2024/1/4
 */
@Data
@Accessors(chain = true)
public class PayRepairResult {
    /** 修复号 */
    private String repairNo;
}
