package org.dromara.daxpay.service.controller.allocation;

import cn.bootx.platform.core.annotation.RequestGroup;
import cn.bootx.platform.core.rest.Res;
import cn.bootx.platform.core.rest.param.PageParam;
import cn.bootx.platform.core.rest.result.PageResult;
import cn.bootx.platform.core.rest.result.Result;
import com.fhs.core.trans.anno.TransMethodResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dromara.daxpay.core.param.allocation.transaction.AllocFinishParam;
import org.dromara.daxpay.service.param.order.allocation.AllocOrderQuery;
import org.dromara.daxpay.service.result.allocation.order.AllocDetailVo;
import org.dromara.daxpay.service.result.allocation.order.AllocOrderVo;
import org.dromara.daxpay.service.service.allocation.AllocationService;
import org.dromara.daxpay.service.service.allocation.order.AllocOrderQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分账订单控制器
 * @author xxm
 * @since 2024/4/7
 */
@Tag(name = "分账订单控制器")
@RestController
@RequestGroup(groupCode = "AllocOrder", groupName = "分账订单", moduleCode = "Alloc", moduleName = "分账管理" )
@RequestMapping("/allocation/order")
@RequiredArgsConstructor
public class AllocOrderController {

    private final AllocOrderQueryService queryService;

    private final AllocationService allocationService;

    @Operation(summary = "分页")
    @GetMapping("/page")
    public Result<PageResult<AllocOrderVo>> page(PageParam pageParam, AllocOrderQuery param){
        return Res.ok(queryService.page(pageParam,param));
    }

    @TransMethodResult
    @Operation(summary = "分账明细列表")
    @GetMapping("/detail/findAll")
    public Result<List<AllocDetailVo>> findDetailsByOrderId(Long orderId){
        return Res.ok(queryService.findDetailsByOrderId(orderId));
    }

    @Operation(summary = "查询详情")
    @GetMapping("/findById")
    public Result<AllocOrderVo> findById(Long id){
        return Res.ok(queryService.findById(id));
    }

    @TransMethodResult
    @Operation(summary = "查询明细详情")
    @GetMapping("/detail/findById")
    public Result<AllocDetailVo> findDetailById(Long id){
        return Res.ok(queryService.findDetailById(id));
    }


    @Operation(summary = "分账完结")
    @PostMapping("/finish")
    public Result<Void> finish(String allocNo){
        AllocFinishParam param = new AllocFinishParam();
        param.setAllocNo(allocNo);
        allocationService.finish(param);
        return Res.ok();
    }
}
