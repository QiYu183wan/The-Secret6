package com.enigma.controller;

import com.enigma.common.R;
import com.enigma.dto.RequestDtos;
import com.enigma.dto.ViewDtos;
import com.enigma.service.ScriptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/scripts")
@RequiredArgsConstructor
public class AdminScriptController {

    private final ScriptService scriptService;

    @GetMapping
    public R<List<ViewDtos.ScriptCard>> list() {
        return R.ok(scriptService.listAll());
    }

    @GetMapping("/{id}")
    public R<ViewDtos.ScriptDetail> detail(@PathVariable Long id) {
        return R.ok(scriptService.adminDetail(id));
    }

    @PostMapping
    public R<ViewDtos.ScriptDetail> create(@Valid @RequestBody RequestDtos.ScriptForm form) {
        return R.ok(scriptService.create(form));
    }

    @PutMapping("/{id}")
    public R<ViewDtos.ScriptDetail> update(@PathVariable Long id,
                                           @Valid @RequestBody RequestDtos.ScriptForm form) {
        return R.ok(scriptService.update(id, form));
    }

    @PostMapping("/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        scriptService.publish(id);
        return R.ok();
    }

    @PostMapping("/{id}/offline")
    public R<Void> offline(@PathVariable Long id) {
        scriptService.offline(id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        scriptService.delete(id);
        return R.ok();
    }

    // ---------------- 角色 ----------------

    @GetMapping("/{id}/characters")
    public R<List<ViewDtos.CharacterItem>> characters(@PathVariable Long id) {
        return R.ok(scriptService.listCharacters(id));
    }

    @PostMapping("/{id}/characters")
    public R<ViewDtos.CharacterItem> createCharacter(@PathVariable Long id,
                                                     @Valid @RequestBody RequestDtos.CharacterForm form) {
        return R.ok(scriptService.createCharacter(id, form));
    }

    @PutMapping("/{id}/characters/{characterId}")
    public R<ViewDtos.CharacterItem> updateCharacter(@PathVariable Long id,
                                                      @PathVariable Long characterId,
                                                      @Valid @RequestBody RequestDtos.CharacterForm form) {
        return R.ok(scriptService.updateCharacter(id, characterId, form));
    }

    @DeleteMapping("/{id}/characters/{characterId}")
    public R<Void> deleteCharacter(@PathVariable Long id, @PathVariable Long characterId) {
        scriptService.deleteCharacter(id, characterId);
        return R.ok();
    }

    // ---------------- 阶段 ----------------

    @GetMapping("/{id}/stages")
    public R<List<ViewDtos.StageItem>> stages(@PathVariable Long id) {
        return R.ok(scriptService.listStages(id));
    }

    @PostMapping("/{id}/stages")
    public R<ViewDtos.StageItem> createStage(@PathVariable Long id,
                                             @Valid @RequestBody RequestDtos.StageForm form) {
        return R.ok(scriptService.createStage(id, form));
    }

    @PutMapping("/{id}/stages/{stageId}")
    public R<ViewDtos.StageItem> updateStage(@PathVariable Long id, @PathVariable Long stageId,
                                             @Valid @RequestBody RequestDtos.StageForm form) {
        return R.ok(scriptService.updateStage(id, stageId, form));
    }

    @DeleteMapping("/{id}/stages/{stageId}")
    public R<Void> deleteStage(@PathVariable Long id, @PathVariable Long stageId) {
        scriptService.deleteStage(id, stageId);
        return R.ok();
    }

    // ---------------- 线索 ----------------

    @GetMapping("/{id}/clues")
    public R<List<ViewDtos.ClueItem>> clues(@PathVariable Long id) {
        return R.ok(scriptService.listClues(id));
    }

    @PostMapping("/{id}/clues")
    public R<ViewDtos.ClueItem> createClue(@PathVariable Long id,
                                           @Valid @RequestBody RequestDtos.ClueForm form) {
        return R.ok(scriptService.createClue(id, form));
    }

    @PutMapping("/{id}/clues/{clueId}")
    public R<ViewDtos.ClueItem> updateClue(@PathVariable Long id, @PathVariable Long clueId,
                                           @Valid @RequestBody RequestDtos.ClueForm form) {
        return R.ok(scriptService.updateClue(id, clueId, form));
    }

    @DeleteMapping("/{id}/clues/{clueId}")
    public R<Void> deleteClue(@PathVariable Long id, @PathVariable Long clueId) {
        scriptService.deleteClue(id, clueId);
        return R.ok();
    }

    // ---------------- 标准答案 ----------------

    @GetMapping("/{id}/answer")
    public R<ViewDtos.AnswerInfo> answer(@PathVariable Long id) {
        return R.ok(scriptService.getAnswer(id));
    }

    @PutMapping("/{id}/answer")
    public R<ViewDtos.AnswerInfo> saveAnswer(@PathVariable Long id,
                                             @Valid @RequestBody RequestDtos.AnswerForm form) {
        return R.ok(scriptService.saveAnswer(id, form));
    }

    // ---------------- 证据关系 ----------------

    @GetMapping("/{id}/relations")
    public R<List<ViewDtos.RelationItem>> relations(@PathVariable Long id) {
        return R.ok(scriptService.listRelations(id));
    }

    @PostMapping("/{id}/relations")
    public R<ViewDtos.RelationItem> createRelation(@PathVariable Long id,
                                                   @Valid @RequestBody RequestDtos.RelationForm form) {
        return R.ok(scriptService.createRelation(id, form));
    }

    @DeleteMapping("/{id}/relations/{relationId}")
    public R<Void> deleteRelation(@PathVariable Long id, @PathVariable Long relationId) {
        scriptService.deleteRelation(id, relationId);
        return R.ok();
    }

    // ---------------- 统计 ----------------

    @GetMapping("/{id}/statistics")
    public R<ViewDtos.StatisticsInfo> statistics(@PathVariable Long id) {
        return R.ok(scriptService.statistics(id));
    }
}
