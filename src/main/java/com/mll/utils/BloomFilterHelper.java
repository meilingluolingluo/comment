import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.mll.service.IShopService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BloomFilterHelper {

    private BloomFilter<Long> bloomFilter;

    @Resource
    private IShopService shopService;

    @PostConstruct
    public void init() {
        List<Long> shopIds = shopService.geAtllShopIds(); // 获取所有店铺ID
        bloomFilter = BloomFilter.create(Funnels.longFunnel(), shopIds.size(), 0.01); // 1%的误判率
        for (Long id : shopIds) {
            bloomFilter.put(id);
        }
    }

    public boolean mightContain(Long id) {
        return bloomFilter.mightContain(id);
    }
}
