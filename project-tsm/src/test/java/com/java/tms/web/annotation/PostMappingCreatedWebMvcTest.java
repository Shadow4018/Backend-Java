package com.java.tms.web.annotation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AC5: a real request to an endpoint using the composed {@link PostMappingCreated}
 * annotation gets exactly the same HTTP behaviour (routing + 201 status) that the two
 * original annotations ({@code @PostMapping} + {@code @ResponseStatus(CREATED)}) gave it
 * before the composition, proven end-to-end via {@code @SpringBootTest}/{@code MockMvc}
 * (the same style used by {@code ProjectSecurityTest}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostMappingCreatedWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void endpointUsingComposedAnnotationRoutesAndRespondsCreated() throws Exception {
        String body = "{\"name\":\"Composed Annotation Project\",\"sourceLanguage\":\"en\",\"targetLanguage\":\"uk\"}";

        mockMvc.perform(post("/api/projects")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Composed Annotation Project"));
    }

    /**
     * AC4, checked structurally rather than at runtime: the merged view of
     * {@code @PostMappingCreated} carries both the mapping annotation and the
     * {@code @ResponseStatus(CREATED)} annotation that
     * {@code AnnotatedElementUtils}/{@code RequestMappingHandlerMapping} rely on — the exact
     * same merged-annotation mechanism that lets {@code @GetMapping} work as a
     * {@code @RequestMapping} shorthand.
     */
    @Test
    void composedAnnotationCarriesBothOriginalAnnotations() throws NoSuchMethodException {
        Method marker = AnnotationCarrierSample.class.getDeclaredMethod("create");
        PostMappingCreated composed = marker.getAnnotation(PostMappingCreated.class);

        assertThat(composed).isNotNull();
        RequestMapping requestMapping = composed.annotationType().getAnnotation(RequestMapping.class);
        assertThat(requestMapping).isNotNull();
        assertThat(requestMapping.method()).containsExactly(RequestMethod.POST);
        assertThat(composed.annotationType().getAnnotation(ResponseStatus.class).value())
                .isEqualTo(org.springframework.http.HttpStatus.CREATED);
    }

    private static final class AnnotationCarrierSample {
        @PostMappingCreated
        void create() {
        }
    }
}
