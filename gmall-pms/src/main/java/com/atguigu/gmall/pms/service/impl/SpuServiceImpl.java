package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.SmsFeign;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.pms.service.SkuImagesService;
import com.atguigu.gmall.pms.service.SpuAttrValueService;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.PmsSkuSaleVo;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.service.SpuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Transactional
    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public PageResultVo querySpu(PageParamVo pageParamVo, Long categoryId) {
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();
        if (categoryId != 0){
            wrapper.eq("category_id",categoryId);
        }
        if (StringUtils.isNotBlank(pageParamVo.getKey())){
            wrapper.and(t->t.like("name",pageParamVo.getKey()).or().like("id",pageParamVo.getKey()));
        }
        return new PageResultVo(this.page(pageParamVo.getPage(),wrapper));
    }

    @Autowired
    private SpuDescMapper spuDescMapper;
    @Autowired
    private SpuAttrValueService spuAttrValueService;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SkuAttrValueService skuAttrValueService;
    @Autowired
    private SmsFeign smsFeign;

    @GlobalTransactional
    @Override
    public void bigSave(SpuVo spu) {
        //保存spu基本信息
        Long spuId = saveSpu(spu);
        //保存spu描述信息（图片）
        saveSpuDesc(spu, spuId);
        //保存spu基本属性信息
        saveBaseAttr(spu, spuId);

        //保存sku相关信息
        saveSku(spu, spuId);
    }

    @Transactional
    public void saveSku(SpuVo spu, Long spuId) {
        //保存sku基本信息
        List<SkuVo> skus = spu.getSkus();
        if (CollectionUtils.isEmpty(skus)){
            return;
        }
        //遍历获取sku的基本信息
        skus.forEach(skuVo -> {
            SkuEntity skuEntity = new SkuEntity();
            BeanUtils.copyProperties(skuVo,skuEntity);
            //获取品牌id和分类id
            skuEntity.setBrandId(spu.getBrandId());
            skuEntity.setCatagoryId(spu.getCategoryId());
            //获取默认图片
            List<String> images = skuVo.getImages();
            if (!CollectionUtils.isEmpty(images)){
                // 设置第一张图片作为默认图片
                skuEntity.setDefaultImage(skuEntity.getDefaultImage()==null?images.get(0):skuEntity.getDefaultImage());
            }
            //获取spuId
            skuEntity.setSpuId(spuId);
            this.skuMapper.insert(skuEntity);
            //获取skuId
            Long skuId = skuEntity.getId();

            //获取图片信息
            if (!CollectionUtils.isEmpty(images)) {
                String defaultImage = images.get(0);
                List<SkuImagesEntity> skuImagesEntities = images.stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    //比较是否为默认图片
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(defaultImage, image) ? 1 : 0);
                    skuImagesEntity.setSort(0);
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setUrl(image);
                    return skuImagesEntity;
                }).collect(Collectors.toList());
                this.skuImagesService.saveBatch(skuImagesEntities);
            }

            //获取sku规格参数
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
            saleAttrs.forEach(saleAttr->{
                //根据sort和skuId获取skuAttrValueEntity
                saleAttr.setSort(0);
                saleAttr.setSkuId(skuId);
            });
            skuAttrValueService.saveBatch(saleAttrs);

            //保存营销信息
            PmsSkuSaleVo pmsSkuSaleVo = new PmsSkuSaleVo();
            BeanUtils.copyProperties(skuVo,pmsSkuSaleVo);
            pmsSkuSaleVo.setSkuId(skuId);
            this.smsFeign.saveSkuSaleInfo(pmsSkuSaleVo);
        });
    }
    @Transactional
    public void saveBaseAttr(SpuVo spu, Long spuId) {
        //保存spu的规格参数
        List<SpuAttrValueVo> baseAttrs = spu.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)) {
            List<SpuAttrValueEntity> spuAttrValueEntities = baseAttrs.stream().map(spuAttrValueVo -> {
                spuAttrValueVo.setSpuId(spuId);
                spuAttrValueVo.setSort(0);
                return spuAttrValueVo;
            }).collect(Collectors.toList());
            this.spuAttrValueService.saveBatch(spuAttrValueEntities);
        }
    }
    @Transactional
    public void saveSpuDesc(SpuVo spu, Long spuId) {
        //保存spu的描述信息
        SpuDescEntity spuDescEntity = new SpuDescEntity();
        //获取新增的spuId并保存
        // 注意：spu_info_desc表的主键是spu_id,需要在实体类中配置该主键不是自增主键ggggggg
        spuDescEntity.setSpuId(spuId);
        // 把商品的图片描述，保存到spu详情中，图片地址以逗号进行分割
        spuDescEntity.setDecript(StringUtils.join(spu.getSpuImages(),","));
        //保存
        this.spuDescMapper.insert(spuDescEntity);
    }
    @Transactional
    public Long saveSpu(SpuVo spu) {
        //保存spu基本信息
        //创建时间手动实现
        spu.setCreateTime(new Date());
        //默认已上架
        spu.setPublishStatus(1);
        spu.setUpdateTime(spu.getCreateTime());
        this.save(spu);
        //获取新增后的spuId
        return spu.getId();
    }

}