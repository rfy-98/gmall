package com.atguigu.gmall.search.service;

import com.atguigu.gmall.search.pojo.SearchParamVo;
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
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


import java.io.IOException;
import java.util.List;

@Service
public class SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;


    public void search(SearchParamVo searchParamVo) {
        try {
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"},buildDSL(searchParamVo));
            SearchResponse response = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
