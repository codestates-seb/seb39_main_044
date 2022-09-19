package main_project.udongs.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main_project.udongs.geoip.GeoIP;
import main_project.udongs.geoip.GeoIPService;
import main_project.udongs.member.dto.MemberDto;
import main_project.udongs.member.entity.Member;
import main_project.udongs.member.mapper.MemberMapper;
import main_project.udongs.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "Member", description = "회원 관련 API")
@Slf4j
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberMapper mapper;
    private final MemberService memberService;
    private final GeoIPService geoIPService;


    @Operation(summary = "회원 등록")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MemberDto.Response.class))))})
    @PostMapping("/signup")
    public ResponseEntity postMember(@Valid @RequestParam(value="ipAddress", required=true) String ipAddress,
                                     @RequestBody MemberDto.Post requestBody) throws Exception{
        log.debug("post member");

        GeoIPService locationService = new GeoIPService();
        GeoIP location = locationService.getLocation(ipAddress);

        requestBody.setLatitude(location.getLatitude());
        requestBody.setLongitude(location.getLongitude());

        Member member = mapper.memberPostToMember(requestBody);
        Member createdMember = memberService.createMember(member);
        MemberDto.Response response = mapper.memberToMemberResponse(createdMember);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @Operation(summary = "단일 회원 조회")
    @ApiResponses(value = @ApiResponse(responseCode = "200", description = "OK"))
    @GetMapping("/{member-id}")
    public ResponseEntity getMember(@PathVariable("member-id") long memberId) {
        log.debug("get member");

        MemberDto.Response response = mapper.memberToMemberResponse(memberService.getMember(memberId));
        return new ResponseEntity(response, HttpStatus.OK);
    }


    @Operation(summary = "전체 회원 조회")
    @ApiResponses(value = @ApiResponse(responseCode = "200", description = "OK"))
    @GetMapping()
    public ResponseEntity getMembers() {
        log.debug("get members");

        return new ResponseEntity("getmembers", HttpStatus.OK);
    }


    @Operation(summary = "회원 정보 수정")
    @ApiResponses(value = @ApiResponse(responseCode = "200", description = "OK"))
    @PatchMapping("/{member-id}")
    public ResponseEntity patchMember(@PathVariable("member-id") Long memberId,
                                      @RequestBody MemberDto.Patch requestBody) {
        log.debug("patch member");
        requestBody.setMemberId(memberId);

        Member member = memberService.updateMember(mapper.memberPatchToMember(requestBody));

        return new ResponseEntity(mapper.memberToMemberResponse(member), HttpStatus.OK);
    }


    @Operation(summary = "회원 탈퇴")
    @ApiResponses(value = @ApiResponse(responseCode = "200", description = "OK"))
    @DeleteMapping("/{member-id}")
    public ResponseEntity deleteMember(@PathVariable("member-id") Long memberId) {
        log.debug("delete member");

        memberService.deleteMember(memberId);

        return new ResponseEntity(HttpStatus.OK);
    }


}
