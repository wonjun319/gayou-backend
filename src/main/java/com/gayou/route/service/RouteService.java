package com.gayou.route.service;

import com.gayou.hashtag.dto.HashtagDto;
import com.gayou.hashtag.model.Hashtag;
import com.gayou.hashtag.repository.HashtagRepository;
import com.gayou.route.dto.RouteHeadDto;
import com.gayou.route.dto.RouteItemDto;
import com.gayou.places.dto.PlacesDto;
import com.gayou.auth.model.User;
import com.gayou.route.model.RouteHashtags;
import com.gayou.route.model.RouteHead;
import com.gayou.route.model.RouteItem;
import com.gayou.places.model.Places;
import com.gayou.places.repository.PlacesRepository;
import com.gayou.auth.repository.UserRepository;
import com.gayou.route.repository.RouteHashtagsRepository;
import com.gayou.route.repository.RouteHeadRepository;
import com.gayou.route.repository.RouteItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RouteService {

    private final RouteHeadRepository routeHeadRepository;
    private final RouteItemRepository routeItemRepository;
    private final PlacesRepository placesRepository;
    private final HashtagRepository hashtagRepository;
    private final RouteHashtagsRepository routeHashtagsRepository;
    private final UserRepository userRepository;

    public RouteService(RouteHeadRepository routeHeadRepository, RouteItemRepository routeItemRepository,
            PlacesRepository placesRepository, RouteHashtagsRepository routeHashtagsRepository,
            HashtagRepository hashtagRepository, UserRepository userRepository) {
        this.routeHeadRepository = routeHeadRepository;
        this.routeItemRepository = routeItemRepository;
        this.placesRepository = placesRepository;
        this.hashtagRepository = hashtagRepository;
        this.routeHashtagsRepository = routeHashtagsRepository;
        this.userRepository = userRepository;
    }

    /**
     * 사용자의 경로(Route)를 저장하는 메서드
     *
     * @param routeDTO - 저장할 경로 정보 (RouteHeadDto)
     * @param email    - 현재 인증된 사용자의 사용자 이메일 (JWT 토큰에서 추출된 값)
     * @return 저장된 경로의 ID
     */
    @Transactional
    public Long saveRoute(RouteHeadDto routeDTO, String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        RouteHead routeHead = new RouteHead();
        routeHead.setUser(user);
        routeHead.setCourseName(routeDTO.getCourseName());
        routeHead.setCreateDate(new Date());
        routeHead.setTown(routeDTO.getTown());
        routeHead.setTotDistance(routeDTO.getTotDistance());

        RouteHead savedRouteHead = routeHeadRepository.save(routeHead);

        List<RouteItemDto> datas = routeDTO.getData();

        List<RouteItem> routeItems = new ArrayList<>();

        for (RouteItemDto data : datas) {
            Places place = placesRepository.findByContentid(data.getContentid().getContentid())
                    .orElseThrow(
                            () -> new RuntimeException("Place with contentid " + data.getContentid() + " not found"));
            RouteItem routeItem = new RouteItem();
            routeItem.setRouteHead(savedRouteHead);
            routeItem.setPlace(place);
            routeItems.add(routeItem);
        }

        routeItemRepository.saveAll(routeItems);

        return savedRouteHead.getId();
    }

    /**
     * 현재 사용자가 저장한 경로 목록을 가져오는 메서드
     *
     * @param email - 현재 인증된 사용자의 사용자 이메일 (JWT 토큰에서 추출된 값)
     * @return 사용자가 저장한 경로 목록 (RouteHeadDto 리스트)
     */
    @Transactional
    public List<RouteHeadDto> getMyRoute(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<RouteHead> getRouteHead = routeHeadRepository.findAllByUserId(user.getId());

        List<RouteHeadDto> dtoData = new ArrayList<>();
        for (RouteHead head : getRouteHead) {
            RouteHeadDto routeHeadDto = new RouteHeadDto();
            routeHeadDto.setId(head.getId());
            routeHeadDto.setTown(head.getTown());
            routeHeadDto.setCourseName(head.getCourseName());
            routeHeadDto.setTotDistance(head.getTotDistance());
            routeHeadDto.setCreateDate(head.getCreateDate());
            routeHeadDto.setUpdateDate(head.getUpdateDate());

            List<RouteItemDto> dtoItemList = new ArrayList<>();
            List<RouteItem> routeItemList = head.getData();
            for (RouteItem item : routeItemList) {
                RouteItemDto routeItemDto = new RouteItemDto();
                routeItemDto.setId(item.getId());

                Places places = item.getPlace();
                PlacesDto placesDto = new PlacesDto(places.getContentid());
                placesDto.setTitle(places.getTitle());
                placesDto.setAddr1(places.getAddr1());
                placesDto.setAddr2(places.getAddr2());
                placesDto.setAreacode(places.getAreacode());
                placesDto.setBooktour(places.getBooktour());
                placesDto.setCat1(places.getCat1());
                placesDto.setCat2(places.getCat2());
                placesDto.setCat3(places.getCat3());
                placesDto.setContenttypeid(places.getContenttypeid());
                placesDto.setCreatedtime(places.getCreatedtime());
                placesDto.setFirstimage(places.getFirstimage());
                placesDto.setFirstimage2(places.getFirstimage2());
                placesDto.setMapx(places.getMapx());
                placesDto.setMapy(places.getMapy());
                placesDto.setModifiedtime(places.getModifiedtime());
                placesDto.setTel(places.getTel());
                placesDto.setOverview(places.getOverview());
                placesDto.setLastUpdated(places.getLastUpdated());

                routeItemDto.setContentid(placesDto);
                dtoItemList.add(routeItemDto);
            }

            routeHeadDto.setData(dtoItemList);

            dtoData.add(routeHeadDto);
        }
        return dtoData;
    }

    @Transactional
    public RouteHeadDto getRoute(Long id) {
        RouteHead head = routeHeadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("route not found"));

        RouteHeadDto routeHeadDto = new RouteHeadDto();
        routeHeadDto.setId(head.getId());
        routeHeadDto.setTown(head.getTown());
        routeHeadDto.setCourseName(head.getCourseName());
        routeHeadDto.setTotDistance(head.getTotDistance());
        routeHeadDto.setContent(head.getContent());
        routeHeadDto.setCreateDate(head.getCreateDate());
        routeHeadDto.setUpdateDate(head.getUpdateDate());

        List<RouteItemDto> dtoItemList = new ArrayList<>();
        List<RouteItem> routeItemList = head.getData();
        for (RouteItem item : routeItemList) {
            RouteItemDto routeItemDto = new RouteItemDto();
            routeItemDto.setId(item.getId());

            Places places = item.getPlace();
            PlacesDto placesDto = new PlacesDto(places.getContentid());
            placesDto.setTitle(places.getTitle());
            placesDto.setAddr1(places.getAddr1());
            placesDto.setAddr2(places.getAddr2());
            placesDto.setAreacode(places.getAreacode());
            placesDto.setBooktour(places.getBooktour());
            placesDto.setCat1(places.getCat1());
            placesDto.setCat2(places.getCat2());
            placesDto.setCat3(places.getCat3());
            placesDto.setContenttypeid(places.getContenttypeid());
            placesDto.setCreatedtime(places.getCreatedtime());
            placesDto.setFirstimage(places.getFirstimage());
            placesDto.setFirstimage2(places.getFirstimage2());
            placesDto.setMapx(places.getMapx());
            placesDto.setMapy(places.getMapy());
            placesDto.setModifiedtime(places.getModifiedtime());
            placesDto.setTel(places.getTel());
            placesDto.setOverview(places.getOverview());
            placesDto.setLastUpdated(places.getLastUpdated());

            routeItemDto.setContentid(placesDto);
            dtoItemList.add(routeItemDto);
        }

        routeHeadDto.setData(dtoItemList);

        return routeHeadDto;
    }

    @Transactional
    public void updateRouteHead(RouteHeadDto routeHeadDto) {
        // 1. RouteHead 엔티티를 가져옴
        RouteHead routeHead = routeHeadRepository.findById(routeHeadDto.getId())
                .orElseThrow(() -> new RuntimeException("Route not found"));

        // 2. 기본 정보 업데이트
        routeHead.setCourseName(routeHeadDto.getCourseName());
        routeHead.setContent(routeHeadDto.getContent());

        // 3. 해시태그가 null인 경우 빈 리스트로 처리
        List<String> tagList = routeHeadDto.getTag() != null ? routeHeadDto.getTag() : Collections.emptyList();

        // 4. 기존 해시태그 조회
        List<String> existingTags = hashtagRepository.findExistingTags(tagList);

        // 5. 중복되지 않은 새로운 태그 필터링
        List<String> nonDuplicateTags = tagList.stream()
                .filter(tag -> !existingTags.contains(tag))
                .collect(Collectors.toList());

        // 6. 새로운 태그가 있으면 저장
        List<Hashtag> newHashtags = nonDuplicateTags.stream()
                .map(tag -> {
                    Hashtag hashtag = new Hashtag();
                    hashtag.setTagName(tag);
                    return hashtag;
                }).collect(Collectors.toList());

        hashtagRepository.saveAll(newHashtags);

        // 7. RouteHashtags 설정 (기존 해시태그 + 새로운 해시태그)
        List<Hashtag> allTags = hashtagRepository.findByTagNameIn(tagList);
        List<RouteHashtags> routeHashtags = allTags.stream()
                .map(hashtag -> {
                    RouteHashtags routeHashtag = new RouteHashtags();
                    routeHashtag.setRouteHead(routeHead);
                    routeHashtag.setHashtag(hashtag);
                    return routeHashtag;
                }).collect(Collectors.toList());

        // 8. 해당 routeHeadId에 대한 기존 RouteHashtags 삭제
        routeHashtagsRepository.deleteByRouteHeadId(routeHead.getId());

        // 9. 새로운 RouteHashtags 삽입
        routeHashtagsRepository.saveAll(routeHashtags);

        // 10. RouteHead 업데이트 저장
        routeHead.setRouteHashtags(routeHashtags); // 연관 관계 설정
        routeHeadRepository.save(routeHead);
    }

}