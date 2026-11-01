package com.editshare.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Set;

@Controller
public class ViewController {

    private static final Set<String> RESERVED_PREFIXES = Set.of(
            "api", "static", "css", "js", "images", "favicon"
    );

    @GetMapping(value = {"/", "/c/{id}", "/c/{id}/", "/{slug:[a-zA-Z0-9][a-zA-Z0-9\\-]*$}"})
    public String forwardToIndex(@PathVariable(required = false) String id,
                                 @PathVariable(required = false) String slug,
                                 HttpServletResponse response) {
        if (slug != null && RESERVED_PREFIXES.contains(slug.toLowerCase())) {
            return "forward:/" + slug;
        }
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        return "forward:/index.html";
    }
}
