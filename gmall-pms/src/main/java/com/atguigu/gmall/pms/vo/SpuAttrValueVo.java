package com.atguigu.gmall.pms.vo;


import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;


@Data
public class SpuAttrValueVo extends SpuAttrValueEntity {

    //selectedValue为集合，重写set方法
    public void setValueSelected(List<Object> valueSelected){
        if (CollectionUtils.isEmpty(valueSelected)) {
            return;
        }
        this.setAttrValue(StringUtils.join(valueSelected,","));
    }
}
