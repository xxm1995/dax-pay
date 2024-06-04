package cn.daxpay.single.sdk.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分账订单处理结果
 * @author xxm
 * @since 2024/4/16
 */
@Getter
@AllArgsConstructor
public enum AllocOrderResultEnum {

    ALL_PENDING("all_pending", "全部处理中"),
    ALL_SUCCESS("all_success", "全部成功"),
    PART_SUCCESS("part_success", "部分成功"),
    ALL_FAILED("all_failed", "全部失败"),
    ;

    private final String code;
    private final String name;
}
