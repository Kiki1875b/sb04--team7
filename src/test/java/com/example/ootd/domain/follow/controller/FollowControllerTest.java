//package com.example.ootd.domain.follow.controller;
//
//import com.example.ootd.domain.follow.dto.Direction;
//import com.example.ootd.domain.follow.dto.FollowCreateRequest;
//import com.example.ootd.domain.follow.dto.FollowDto;
//import com.example.ootd.domain.follow.dto.FollowListResponse;
//import com.example.ootd.domain.follow.dto.FollowSummaryDto;
//import com.example.ootd.domain.follow.service.FollowService;
//import com.example.ootd.domain.user.dto.UserSummary;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.List;
//import java.util.UUID;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.doNothing;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(FollowController.class)
//@DisplayName("FollowController 테스트")
//class FollowControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockitoBean
//    private FollowService followService;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    private UUID followerId;
//    private UUID followeeId;
//    private UUID followId;
//    private FollowDto followDto;
//    private FollowSummaryDto followSummaryDto;
//    private FollowListResponse followListResponse;
//
//    @BeforeEach
//    void setUp() {
//        followerId = UUID.randomUUID();
//        followeeId = UUID.randomUUID();
//        followId = UUID.randomUUID();
//
//        UserSummary followerSummary = new UserSummary(followerId, "팔로워", "profile1.jpg");
//        UserSummary followeeSummary = new UserSummary(followeeId, "팔로위", "profile2.jpg");
//
//        followDto = new FollowDto(followId, followerSummary, followeeSummary);
//        followSummaryDto = new FollowSummaryDto(followeeId, 5L, 10L, true, followId, false);
//        followListResponse = new FollowListResponse(
//            List.of(followDto),
//            "nextCursor",
//            UUID.randomUUID(),
//            true,
//            1,
//            "createdAt",
//            Direction.ASCENDING
//        );
//    }
//
//    @Nested
//    @DisplayName("팔로우 생성")
//    class CreateFollow {
//
//        @Test
//        @DisplayName("팔로우 생성 성공")
//        void createFollow_Success() throws Exception {
//            // given
//            FollowCreateRequest request = new FollowCreateRequest(followerId, followeeId);
//            given(followService.createFollow(any(FollowCreateRequest.class))).willReturn(followDto);
//
//            // when & then
//            mockMvc.perform(post("/api/follows")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.id").value(followId.toString()))
//                .andExpect(jsonPath("$.follower.userId").value(followerId.toString()))
//                .andExpect(jsonPath("$.followee.userId").value(followeeId.toString()));
//        }
//    }
//
//    @Nested
//    @DisplayName("팔로우 요약 정보 조회")
//    class GetSummary {
//
//        @Test
//        @DisplayName("팔로우 요약 정보 조회 성공")
//        void getSummary_Success() throws Exception {
//            // given
//            given(followService.getSummaryFollow(followerId)).willReturn(followSummaryDto);
//
//            // when & then
//            mockMvc.perform(get("/api/follows/summary")
//                    .param("userId", followerId.toString()))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.followerCount").value(5))
//                .andExpect(jsonPath("$.followingCount").value(10));
//        }
//    }
//
//    @Nested
//    @DisplayName("팔로잉 목록 조회")
//    class GetFollowings {
//
//        @Test
//        @DisplayName("팔로잉 목록 조회 성공")
//        void getFollowings_Success() throws Exception {
//            // given
//            given(followService.getFollowingList(any(), eq(followerId))).willReturn(followListResponse);
//
//            // when & then
//            mockMvc.perform(get("/api/follows/followings")
//                    .param("followingId", followerId.toString())
//                    .param("limit", "10"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data").isArray())
//                .andExpect(jsonPath("$.data[0].id").value(followId.toString()))
//                .andExpect(jsonPath("$.hasNext").value(true))
//                .andExpect(jsonPath("$.totalCount").value(1));
//        }
//
//        @Test
//        @DisplayName("팔로잉 목록 조회 - 검색 조건 포함")
//        void getFollowings_WithSearchCondition() throws Exception {
//            // given
//            given(followService.getFollowingList(any(), eq(followerId))).willReturn(followListResponse);
//
//            // when & then
//            mockMvc.perform(get("/api/follows/followings")
//                    .param("followingId", followerId.toString())
//                    .param("limit", "10")
//                    .param("nameLike", "test"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data").isArray());
//        }
//    }
//
//    @Nested
//    @DisplayName("팔로워 목록 조회")
//    class GetFollowers {
//
//        @Test
//        @DisplayName("팔로워 목록 조회 성공")
//        void getFollowers_Success() throws Exception {
//            // given
//            given(followService.getFollowerList(any(), eq(followeeId))).willReturn(followListResponse);
//
//            // when & then
//            mockMvc.perform(get("/api/follows/followers")
//                    .param("followeeId", followeeId.toString())
//                    .param("limit", "10"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data").isArray())
//                .andExpect(jsonPath("$.data[0].id").value(followId.toString()))
//                .andExpect(jsonPath("$.hasNext").value(true))
//                .andExpect(jsonPath("$.totalCount").value(1));
//        }
//
//        @Test
//        @DisplayName("팔로워 목록 조회 - 커서 페이지네이션")
//        void getFollowers_WithCursor() throws Exception {
//            // given
//            given(followService.getFollowerList(any(), eq(followeeId))).willReturn(followListResponse);
//
//            // when & then
//            mockMvc.perform(get("/api/follows/followers")
//                    .param("followeeId", followeeId.toString())
//                    .param("limit", "5")
//                    .param("cursor", "someCursor")
//                    .param("orderBy", "name")
//                    .param("direction", "DESCENDING"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data").isArray());
//        }
//    }
//
//    @Nested
//    @DisplayName("팔로우 삭제")
//    class DeleteFollow {
//
//        @Test
//        @DisplayName("팔로우 삭제 성공")
//        void deleteFollow_Success() throws Exception {
//            // given
//            doNothing().when(followService).deleteFollow(followId);
//
//            // when & then
//            mockMvc.perform(delete("/api/follows/{followId}", followId))
//                .andExpect(status().isNoContent());
//        }
//    }
//}