package com.atguigu.gmall.search;

import com.atguigu.gmall.api.GmallWmsApi;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsFeign;
import com.atguigu.gmall.search.feign.GmallWmsFeign;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@SpringBootTest
class GmallSearchApplicationTests {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private GmallWmsFeign gmallWmsFeign;

    @Autowired
    private GmallPmsFeign gmallPmsFeign;

    @Autowired
    private GoodsRepository goodsRepository;

    @Test
    void contextLoads() {
        //创建索引及映射
        this.restTemplate.createIndex(Goods.class);
        this.restTemplate.putMapping(Goods.class);

        Integer pageNum = 1;
        Integer pageSize = 100;

        do {
            //获取spus
            PageParamVo pageParamVo = new PageParamVo();
            pageParamVo.setPageNum(pageNum);
            pageParamVo.setPageSize(pageSize);
            ResponseVo<List<SpuEntity>> spusList = this.gmallPmsFeign.querySpusByPage(pageParamVo);
            List<SpuEntity> spus = spusList.getData();

            //遍历spu获取sku
            spus.forEach(spuEntity -> {
                ResponseVo<List<SkuEntity>> skuGrep = this.gmallPmsFeign.querySku(spuEntity.getId());
                List<SkuEntity> skus = skuGrep.getData();
                //将sku转换为Goods
                if (!CollectionUtils.isEmpty(skus)) {
                    List<Goods> goodsList = skus.stream().map(skuEntity -> {
                        Goods goods = new Goods();
                        //获取spu的搜索属性及值
                        ResponseVo<List<SpuAttrValueEntity>> spuAttrValues = this.gmallPmsFeign.querySearchAttrValueBySpuId(spuEntity.getId());
                        List<SpuAttrValueEntity> spuAttrValueEntities = spuAttrValues.getData();
                        //创建搜索属性及值的集合
                        List<SearchAttrValue> searchAttrValues = new ArrayList<>();
                        if (!CollectionUtils.isEmpty(spuAttrValueEntities)){
                            searchAttrValues = spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                                SearchAttrValue searchAttrValue = new SearchAttrValue();
                                searchAttrValue.setAttrId(spuAttrValueEntity.getAttrId());
                                searchAttrValue.setAttrName(spuAttrValueEntity.getAttrName());
                                searchAttrValue.setAttrValue(spuAttrValueEntity.getAttrValue());
                                return searchAttrValue;
                            }).collect(Collectors.toList());
                        }

                        //获取sku搜索属性及值
                        ResponseVo<List<SkuAttrValueEntity>> skuAttrValues = this.gmallPmsFeign.querySearchAttrValueBySkuId(skuEntity.getId());
                        List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValues.getData();
                        //创建搜索属性及值的集合
                        List<SearchAttrValue> searchSkuAttrValues = new ArrayList<>();
                        if (!CollectionUtils.isEmpty(skuAttrValueEntities)){
                            searchSkuAttrValues = skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                                SearchAttrValue searchAttrValue = new SearchAttrValue();
                                searchAttrValue.setAttrValue(skuAttrValueEntity.getAttrValue());
                                searchAttrValue.setAttrName(skuAttrValueEntity.getAttrName());
                                searchAttrValue.setAttrId(skuAttrValueEntity.getAttrId());
                                return searchAttrValue;
                            }).collect(Collectors.toList());
                        }
                        //将搜索属性保存到goods中
                        searchAttrValues.addAll(searchSkuAttrValues);
                        goods.setSearchAttrs(searchAttrValues);

                        //查询品牌
                        ResponseVo<BrandEntity> brandEntityResponseVo = this.gmallPmsFeign.queryBrandById(skuEntity.getBrandId());
                        BrandEntity brandEntity = brandEntityResponseVo.getData();
                        if (brandEntity != null){
                            goods.setBrandId(brandEntity.getId());
                            goods.setBrandName(brandEntity.getName());
                            goods.setLogo(brandEntity.getLogo());
                        }

                        //查询分类
                        ResponseVo<CategoryEntity> categoryEntityResponseVo = this.gmallPmsFeign.queryCategoryById(skuEntity.getCatagoryId());
                        CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
                        if (categoryEntity != null){
                            goods.setCategoryId(categoryEntity.getId());
                            goods.setCategoryName(categoryEntity.getName());
                        }
                        goods.setCreateTime(spuEntity.getCreateTime());
                        goods.setSkuId(skuEntity.getId());
                        goods.setSales(0l);
                        goods.setPrice(skuEntity.getPrice().doubleValue());
                        goods.setDefaultImage(skuEntity.getDefaultImage());

                        //查询销售
                        ResponseVo<List<WareSkuEntity>> wareSku = this.gmallWmsFeign.queryWareSku(skuEntity.getId());
                        List<WareSkuEntity> wareSkuEntityList = wareSku.getData();
                        if (!CollectionUtils.isEmpty(wareSkuEntityList)){
                            boolean b = wareSkuEntityList.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0);
                            goods.setStore(b);
                        }
                        goods.setTitle(skuEntity.getTitle());
                        return goods;
                    }).collect(Collectors.toList());
                    this.goodsRepository.saveAll(goodsList);
                }
            });
            pageSize = spus.size();
            pageNum++;
        }while (pageSize == 100);

    }

}
