package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;



import java.io.IOException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;


    public SearchResponseVo search(SearchParamVo searchParamVo) {
        try {
            //查询条件
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"},buildDSL(searchParamVo));
            //执行查询
            SearchResponse response = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            //结果集解析
            SearchResponseVo searchResponseVo = this.parseResult(response);
            //进行分页
            searchResponseVo.setPageNum(searchParamVo.getPageNum());
            searchResponseVo.setPageSize(searchParamVo.getPageSize());
            return searchResponseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    //结果集解析
    private SearchResponseVo parseResult(SearchResponse response) {
        SearchResponseVo responseVo = new SearchResponseVo();
        //获取结果集
        SearchHits searchHits = response.getHits();
        //获取总的命中纪录数
        long total = searchHits.getTotalHits();
        responseVo.setTotal(total);
        //获取内层hit
        SearchHit[] hits = searchHits.getHits();
        List<Goods> goodsList = Stream.of(hits).map(hit->{
            //获取内层hits下的_source
            String source = hit.getSourceAsString();
            //将source反序列化为goods
            Goods goods = JSON.parseObject(source, Goods.class);
            //解析高亮title替换普通title
            //获取field集合 字段名：值   每个元素为数组
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            //获取其中的数组
            HighlightField field = highlightFields.get("title");
            //获取title
            String title = field.getFragments()[0].toString();
            goods.setTitle(title);
            return goods;
        }).collect(Collectors.toList());
        responseVo.setGoodsList(goodsList);

        //嵌套聚合解析之品牌
        //获取嵌套聚合的集合数据
        Map<String, Aggregation> stringAggregationMap = response.getAggregations().asMap();
        //获取brandIdAgg并转换为Long
        ParsedLongTerms brandIdAgg =(ParsedLongTerms) stringAggregationMap.get("brandIdAgg");
        //获取桶
        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();
        //判断brandIdAgg中是否为空   是否有子聚合
        if (!CollectionUtils.isEmpty(buckets)){
            List<BrandEntity> brandEntities = buckets.stream().map(bucket->{
                BrandEntity brandEntity = new BrandEntity();
                //获取brandId
                long brandId = ((Terms.Bucket) bucket).getKeyAsNumber().longValue();
                brandEntity.setId(brandId);
                //获取子聚合
                Map<String, Aggregation> brandAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
                ParsedStringTerms brandNameAgg = (ParsedStringTerms) brandAggregationMap.get("brandNameAgg");
                ParsedStringTerms logoAgg = (ParsedStringTerms) brandAggregationMap.get("logoAgg");
                //获取brandName
                String brandName = brandNameAgg.getBuckets().get(0).toString();
                brandEntity.setName(brandName);
                //获取logo
                List<? extends Terms.Bucket> logoBucket = logoAgg.getBuckets();
                if (!CollectionUtils.isEmpty(logoBucket)){
                    String logo = logoBucket.get(0).toString();
                    brandEntity.setLogo(logo);
                }
                return brandEntity;
            }).collect(Collectors.toList());
            responseVo.setBrands(brandEntities);
        }

        //嵌套聚合解析之分类
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) stringAggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryIdAggBuckets = categoryIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(categoryIdAggBuckets)){
            List<CategoryEntity> categoryEntityList = categoryIdAggBuckets.stream().map(categoryIdAggBucket->{
                CategoryEntity categoryEntity = new CategoryEntity();
                //获取categoryId
                long categoryId = ((Terms.Bucket) categoryIdAggBucket).getKeyAsNumber().longValue();
                categoryEntity.setId(categoryId);
                //获取子聚合
                Map<String, Aggregation> categoryAggregationMap = ((Terms.Bucket) categoryIdAggBucket).getAggregations().asMap();
                //获取categoryNameAgg
                ParsedStringTerms categoryNameAgg = (ParsedStringTerms) categoryAggregationMap.get("categoryNameAgg");
                //获取categoryName
                String categoryName = categoryNameAgg.getBuckets().get(0).toString();
                categoryEntity.setName(categoryName);
                return categoryEntity;
            }).collect(Collectors.toList());
            responseVo.setCategories(categoryEntityList);
        }

        //嵌套聚合解析之规格参数
        ParsedNested searchAttrsAgg = (ParsedNested)stringAggregationMap.get("searchAttrsAgg");
        //获取子聚合attrIdAgg
        ParsedLongTerms attrIdAgg =(ParsedLongTerms) searchAttrsAgg.getAggregations().get("attrIdAgg");
        //获取桶
        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(attrIdAggBuckets)){
            List<SearchResponseAttrVo> filters = attrIdAggBuckets.stream().map(attrIdAggBucket->{
                SearchResponseAttrVo attrVo = new SearchResponseAttrVo();
                //获取attrId
                long attrId = ((Terms.Bucket) attrIdAggBucket).getKeyAsNumber().longValue();
                attrVo.setAttrId(attrId);

                //获取attrIdAgg下的子聚合
                Map<String, Aggregation> attrIdAggMap = ((Terms.Bucket) attrIdAggBucket).getAggregations().asMap();
                //获取attrNameAgg
                ParsedStringTerms attrNameAgg = (ParsedStringTerms) attrIdAggMap.get("attrNameAgg");
                //获取attrName
                String attrName = attrNameAgg.getBuckets().get(0).toString();
                attrVo.setAttrName(attrName);

                //获取attrValueAgg
                ParsedStringTerms attrValueAgg = (ParsedStringTerms) attrIdAggMap.get("attrValueAgg");
                //获取attrValue
                String attrValue = attrValueAgg.getBuckets().get(0).toString();



                return attrVo;
            }).collect(Collectors.toList());
        }


        return responseVo;
    }


    //构建dsl语句
    private SearchSourceBuilder buildDSL(SearchParamVo searchParamVo) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //获取查询关键字
        String keyword = searchParamVo.getKeyword();
        if (StringUtils.isBlank(keyword)){
            return sourceBuilder;
        }

        //创建查询条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //匹配查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("title",keyword).operator(Operator.AND));
        //过滤查询
        //品牌过滤查询
        List<Long> brandId = searchParamVo.getBrandId();
        if (!CollectionUtils.isEmpty(brandId)){
            boolQueryBuilder.filter(QueryBuilders.termQuery("brandId",brandId));
        }
        //分类过滤查询
        Long cid = searchParamVo.getCid();
        if (cid != null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("categoryId",cid));
        }
        //价格区间过滤查询
        Double priceFrom = searchParamVo.getPriceFrom();
        Double priceTo = searchParamVo.getPriceTo();
        if (priceFrom != null || priceTo != null){
            //获取不为空的价格值
            RangeQueryBuilder price = QueryBuilders.rangeQuery("price");
            //如果价格小的不为空，则搜索的价格大于等于最小价格
            if (priceFrom != null){
                price.gte(priceFrom);
            }
            //如果价格大的不为空，则搜索的价格小于等于最大价格
            if (priceTo != null){
                price.lte(priceTo);
            }
        }
        //是否有货
        Boolean store = searchParamVo.getStore();
        if (store != null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("store",store));
        }
        // 规格参数的过滤 props=5:高通-麒麟&props=6:骁龙865-硅谷1000
        List<String> props = searchParamVo.getProps();
        if (!CollectionUtils.isEmpty(props)){
            //遍历获取规格参数
            props.forEach(prop->{
                //切除props中的： 分成集合
                String[] attr = StringUtils.split(prop, ":");
                if (attr !=null || attr.length == 2){
                   String attrId = attr[0];
                   String attrValue = attr[1];
                   String[] attrValues = StringUtils.split(attrValue, "-");
                    BoolQueryBuilder boolBuilder = QueryBuilders.boolQuery();
                    //匹配查询
                    boolBuilder.must(QueryBuilders.termQuery("searchAttrs.attrId",attrId));
                    boolBuilder.must(QueryBuilders.termQuery("searchAttrs.attrValue",attrValues));
                    //过滤查询  嵌套查询
                    boolBuilder.filter(QueryBuilders.nestedQuery("searchAttrs",boolBuilder, ScoreMode.None));
                }
            });
        }
        sourceBuilder.query(boolQueryBuilder);


        //构建排序
        Integer sort = searchParamVo.getSort();
        String filed = "";
        SortOrder order = null;
        switch (sort){
            case 1 :
                filed = "price";
                order = SortOrder.ASC;
                break;
            case 2 :
                filed = "price";
                order = SortOrder.DESC;
                break;
            case 3 :
                filed = "createTime";
                order = SortOrder.DESC;
                break;
            case 4 :
                filed = "sales";
                order = SortOrder.DESC;
                break;
            default:
                //不写导致空指针
                filed = "_source";
                order = SortOrder.DESC;
                break;
        }
        sourceBuilder.sort(filed,order);

        //构建分页
        Integer pageNum = searchParamVo.getPageNum();
        Integer pageSize = searchParamVo.getPageSize();
        sourceBuilder.from((pageNum-1)*pageSize);
        sourceBuilder.size(pageSize);

        //构建高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<b style='color:red'>").postTags("</b>"));

        //构建聚合
        //构建品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                //字聚合
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo"))
        );

        //构建分类聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                //字聚合
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName"))
        );

        //构建规格参数聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("searchAttrsAgg","searchAttrs")
                //字聚合
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        //字聚合下的子聚合
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue")))
        );

        //构建结果集参数
        //去除值为空的属性  增加性能
        sourceBuilder.fetchSource(new String[]{"skuId", "title", "price", "defaultImage"},null);

        System.out.println(sourceBuilder);
        return sourceBuilder;
    }
}
