package com.atguigu.tingshu;

import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.model.album.TrackStat;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@SpringBootTest
public class AlbumServiceTest {
    @Resource
    private AlbumStatMapper albumStatMapper;

    @Resource
    private TrackStatMapper trackStatMapper;

    @Test
    public void updateAlbumStat() {
        List<AlbumStat> albumStatList = albumStatMapper.selectList(null);
        Map<String, List<AlbumStat>> typeToAlbumStatMap =
                albumStatList.stream().collect(Collectors.groupingBy(AlbumStat::getStatType));
        typeToAlbumStatMap.forEach((type, albumStats) -> {
            for (AlbumStat albumStat : albumStats) {
                switch (type) {
                    case "0401":
                        albumStat.setStatNum(new Random().nextInt(1000));
                        break;
                    case "0402":
                        albumStat.setStatNum(new Random().nextInt(300));
                        break;
                    case "0403":
                        albumStat.setStatNum(new Random().nextInt(100));
                        break;
                    case "0404":
                        albumStat.setStatNum(new Random().nextInt(800));
                        break;
                }
                this.albumStatMapper.updateById(albumStat);
            }
        });
    }

    @Test
    public void updateTrackStat() {
        List<TrackStat> trackStatList = trackStatMapper.selectList(null);
        Map<String, List<TrackStat>> typeToTrackStatMap =
                trackStatList.stream().collect(Collectors.groupingBy(TrackStat::getStatType));
        typeToTrackStatMap.forEach((type, trackStats) -> {
            for (TrackStat trackStat : trackStats) {
                switch (type) {
                    case "0701":
                        trackStat.setStatNum(new Random().nextInt(1000));
                        break;
                    case "0702":
                        trackStat.setStatNum(new Random().nextInt(300));
                        break;
                    case "0703":
                        trackStat.setStatNum(new Random().nextInt(100));
                        break;
                    case "0704":
                        trackStat.setStatNum(new Random().nextInt(800));
                        break;
                }
                this.trackStatMapper.updateById(trackStat);
            }
        });

    }
}
