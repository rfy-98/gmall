package com.atguigu.gmall.pms.controller;

import java.util.List;

import com.atguigu.gmall.pms.vo.SpuVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gmall.pms.entity.SpuEntity;
import com.atguigu.gmall.pms.service.SpuService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.PageParamVo;

/**
 * spu信息
 *
 * @author yige
 * @email 1020972669@qq.com
 * @date 2020-12-14 19:29:38
 */
@Api(tags = "spu信息 管理")
@RestController
@RequestMapping("pms/spu")
public class SpuController {

    @Autowired
    private SpuService spuService;


    //分页查询已上架的spu信息
    @PostMapping("page")
    public ResponseVo<List<SpuEntity>> querySpusByPage(@RequestBody PageParamVo pageParamVo){
        PageResultVo page = this.spuService.queryPage(pageParamVo);
        List<SpuEntity> list =(List<SpuEntity>) page.getList();
        return ResponseVo.ok(list);
    }


    @ApiOperation("spu商品信息查询")
    @GetMapping("category/{categoryId}")
    public ResponseVo<PageResultVo> querySpu(PageParamVo pageParamVo , @PathVariable("categoryId")Long categoryId){
        PageResultVo pageResultVo = this.spuService.querySpu(pageParamVo,categoryId);
        return ResponseVo.ok(pageResultVo);
    }

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> querySpuByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = spuService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id){
		SpuEntity spu = spuService.getById(id);

        return ResponseVo.ok(spu);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody SpuVo spu){
		spuService.bigSave(spu);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody SpuEntity spu){
		spuService.updateById(spu);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		spuService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
