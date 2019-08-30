package com.xiupeilian.carpart.service.impl;

import com.xiupeilian.carpart.mapper.CityMapper;
import com.xiupeilian.carpart.model.City;
import com.xiupeilian.carpart.service.CityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CityServiceImpl implements CityService {

    @Autowired
    private CityMapper cityMapper;

    @Override
    @Cacheable(value="canglaoshi")
    public List<City> findCitiesByParentId(Integer parentId) {
        return cityMapper. findCitiesByParentId( parentId);
    }


    /**
     * @Description: 需要定义规则，手动维护缓存(删除或清空)
     * @Author:      Administrator
     * @Param:       [id]
     * @Return       void
     **/
    @Override
    @CacheEvict(value="canglaoshi",allEntries = true)
    public void deleteCityById(int id) {
        cityMapper.deleteByPrimaryKey(id);
    }

}
