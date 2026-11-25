package com.enigma.controller;

import com.enigma.common.R;
import com.enigma.dto.ViewDtos;
import com.enigma.service.ScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scripts")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptService scriptService;

    @GetMapping
    public R<List<ViewDtos.ScriptCard>> list() {
        return R.ok(scriptService.listPublished());
    }

    @GetMapping("/{id}")
    public R<ViewDtos.ScriptDetail> detail(@PathVariable("id") Long id) {
        return R.ok(scriptService.getPublishedDetail(id));
    }
}
