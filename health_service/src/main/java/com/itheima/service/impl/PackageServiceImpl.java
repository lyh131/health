package com.itheima.service.impl;

import com.alibaba.dubbo.config.annotation.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.dao.PackageDao;
import com.itheima.pojo.Package;
import com.itheima.service.PackageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.Map;

@Service(interfaceClass = PackageService.class)
public class PackageServiceImpl implements PackageService {

    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private PackageDao packageDao;

    @Override
    @Transactional
    public void add(Package pkg, Integer[] checkgroupIds) {
        // 添加套餐，插入套餐表
        packageDao.add(pkg);
        // 获取套餐的ID
        Integer pkgId = pkg.getId();
        // 循环遍历检查组的编号，插入关系
        if(null != checkgroupIds){
            for (Integer checkgroupId : checkgroupIds) {
                packageDao.addPackageAndCheckGroup(pkgId, checkgroupId);
            }
        }
    }

    /**
     * 查询所有套餐
     * @return
     */
    @Override
    public List<Package> findAll() throws Exception {
        //从redis中获取所有分类信息
        Jedis jedis = jedisPool.getResource();
        String key="packageList";
        String jsonStr = jedis.get(key);

        ObjectMapper mapper = new ObjectMapper();
        //原redis中是否有数据
        if (null==jsonStr) {
            //从数据库查询获取
            List<Package> packagelist= packageDao.findAll();
            jsonStr = mapper.writeValueAsString(packagelist);
            //5.将jsonStr存储到redis中
            jedis.set(key,jsonStr);
        }
        //关闭连接
        jedis.close();
        //将jsonStr转换成List<Category>
        List<Package> list = mapper.readValue(jsonStr, new TypeReference<List<Package>>() {});
        return list;
    }

    /**
     * 获取套餐详情信息
     * @param id
     * @return
     */
    @Override
    public Package getPackageDetail(int id) throws Exception {
        //从redis中获取所有分类信息
        Jedis jedis = jedisPool.getResource();
        String key="packageDetil"+id;
        String jsonStr = jedis.get(key);

        ObjectMapper mapper = new ObjectMapper();
        Package pkg=null;

        if (null==jsonStr) {
            //从数据库查询获取
            pkg= packageDao.getPackageDetail(id);
            jsonStr = mapper.writeValueAsString(pkg);
            //5.将jsonStr存储到redis中
            jedis.set(key,jsonStr);
        }
         pkg = mapper.readValue(jsonStr, Package.class);
        //关闭连接
        jedis.close();
        return pkg;
    }

    /**
     * 通过编号获取套餐信息
     * @param id
     * @return
     */
    @Override
    public Package findById(int id) {
        return packageDao.findById(id);
    }

    /**
     * 获取套餐预约数据
     * @return
     */
    @Override
    public List<Map<String, Object>> getPackageReport() {
        return packageDao.getPackageReport();
    }
}
