import com.mll.RedisApplication;
import com.mll.entity.Shop;
import com.mll.service.IShopService;
import com.mll.service.impl.ShopServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest(classes = RedisApplication.class)
public class MainTest {
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    private  IShopService shopService;
    @Test
    public void loadShopData() {
        List<Shop> list = shopService.list();
        Map<Long, List<Shop>> map = list.stream().collect(
                Collectors.groupingBy(Shop::getTypeId)
        );
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            Long typeId = entry.getKey();
            String key = "shop:geo:" + typeId;
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());

            for (Shop shop : value) {
                //stringRedisTemplate.opsForGeo().add(key,new Point(shop.getX(),shop.getY()),shop.getId().toString() );
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())
                ));
            }
            stringRedisTemplate.opsForGeo().add(key, locations);
        }

    }
}
