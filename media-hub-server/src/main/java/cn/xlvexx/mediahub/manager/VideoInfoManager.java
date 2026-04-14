package cn.xlvexx.mediahub.manager;

import cn.xlvexx.mediahub.entity.VideoInfo;
import cn.xlvexx.mediahub.mapper.VideoInfoMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 视频信息数据访问层，封装视频缓存的有效期查询操作
 *
 * @author 林风自在
 * @date 2026-04-03
 */
@Component
public class VideoInfoManager extends ServiceImpl<VideoInfoMapper, VideoInfo> {

    public VideoInfo findValidCache(String urlHash) {
        return lambdaQuery()
                .eq(VideoInfo::getUrlHash, urlHash)
                .gt(VideoInfo::getExpireAt, LocalDateTime.now())
                .last("LIMIT 1")
                .one();
    }
}
