package main_project.udongs.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main_project.udongs.member.dto.MemberDto;
import main_project.udongs.member.entity.Member;
import main_project.udongs.member.service.MemberService;
import main_project.udongs.study.dto.StudyDto;
import main_project.udongs.study.entity.Study;
import main_project.udongs.study.mapper.StudyMapper;
import main_project.udongs.study.service.StudyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@Tag(name = "STUDY", description = "스터디 관련 API")
@RequestMapping("/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;
    private final StudyMapper mapper;
    private final MemberService memberService;

    @Operation(summary = "스터디 모집 등록")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = StudyDto.Response.ResponseBuilder.class))))})
    @PostMapping("{member-id}/recruit")
    public ResponseEntity postStudy(@Valid @PathVariable("member-id") Long memberId,
                                        @RequestBody StudyDto.Post requestBody) {
        log.debug("POST STUDY");

        Study study = mapper.studyPostToStudy(requestBody);
        study.setCreatedAt(LocalDateTime.now());

        //등록시 스터디장의 위치정보, 스터디장 id번호 반환
        Member member = memberService.findVerifiedMember(memberId);
        study.setCity(member.getCity());
        study.setMember(member);

        Study savedStudy = studyService.createStudy(study);
        StudyDto.Response response = mapper.studyToStudyResponse(savedStudy);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    @Operation(summary = "스터디 조회")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = StudyDto.class))))})
    @GetMapping("/{study-id}")
    public ResponseEntity postStudy(@Valid @PathVariable("study-id") Long studyId) {
        log.debug("GET STUDY");

        StudyDto.Response response = mapper.studyToStudyResponse(studyService.findVerifiedStudy(studyId));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @Operation(summary = "전체 스터디 조회")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = StudyDto.class))))})
    @GetMapping
    public ResponseEntity<List<StudyDto.Response>> getStudies() {
        log.debug("GET ALL STUDIES");

        List<StudyDto.Response> list = mapper.studiesToStudyResponse(studyService.getStudies());

        return new ResponseEntity<>(list, HttpStatus.OK);
    }




}