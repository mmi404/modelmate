package com.modelmate;

import com.modelmate.category.CategoryRepository;
import com.modelmate.model.ModelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ModelMateApplicationTests extends AbstractIntegrationTest {

    @Autowired
    CategoryRepository categories;

    @Autowired
    ModelRepository models;

    @Test
    void contextLoadsAndSeedDataApplied() {
        assertThat(categories.count()).isEqualTo(12);
        assertThat(models.count()).isEqualTo(12);
        assertThat(categories.findBySlug("computer-vision")).isPresent();
        assertThat(models.findBySlug("gpt-4")).isPresent();
    }
}
