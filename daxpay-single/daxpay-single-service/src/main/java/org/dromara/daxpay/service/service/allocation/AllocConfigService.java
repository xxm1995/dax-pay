package org.dromara.daxpay.service.service.allocation;

import cn.bootx.platform.core.exception.DataNotExistException;
import cn.bootx.platform.core.exception.RepetitiveOperationException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.daxpay.service.convert.allocation.AllocConfigConvert;
import org.dromara.daxpay.service.dao.config.AllocConfigManger;
import org.dromara.daxpay.service.entity.allocation.AllocConfig;
import org.dromara.daxpay.service.param.allocation.AllocConfigParam;
import org.dromara.daxpay.service.result.allocation.AllocConfigResult;
import org.springframework.stereotype.Service;

/**
 * 分账配置
 * @author xxm
 * @since 2024/12/9
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AllocConfigService {
    private final AllocConfigManger allocConfigManger;

    /**
     * 新增
     */
    public void save(AllocConfigParam param) {
        // 判断是否已经存在
        if (allocConfigManger.existsByAppId(param.getAppId())){
            throw new RepetitiveOperationException("该应用已存在收银台配置");
        }

        AllocConfig entity = AllocConfigConvert.CONVERT.toEntity(param);
        allocConfigManger.save(entity);
    }

    /**
     * 修改
     */
    public void update(AllocConfigParam param) {
        AllocConfig config = allocConfigManger.findById(param.getId())
                .orElseThrow(() -> new DataNotExistException("分账配置不存在"));
        BeanUtil.copyProperties(config, param, CopyOptions.create().ignoreNullValue());
        allocConfigManger.updateById(config);
    }

    /**
     * 根据AppId查询
     */
    public AllocConfigResult findByAppId(String appId) {
        return allocConfigManger.findByAppId(appId).map(AllocConfig::toResult)
                .orElse(new AllocConfigResult().setAutoAlloc(false));
    }


}
