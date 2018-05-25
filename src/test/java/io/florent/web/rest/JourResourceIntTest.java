package io.florent.web.rest;

import io.florent.QuoiQuonMangeApp;

import io.florent.domain.Jour;
import io.florent.repository.JourRepository;
import io.florent.web.rest.errors.ExceptionTranslator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static io.florent.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the JourResource REST controller.
 *
 * @see JourResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = QuoiQuonMangeApp.class)
public class JourResourceIntTest {

    @Autowired
    private JourRepository jourRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restJourMockMvc;

    private Jour jour;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final JourResource jourResource = new JourResource(jourRepository);
        this.restJourMockMvc = MockMvcBuilders.standaloneSetup(jourResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Jour createEntity(EntityManager em) {
        Jour jour = new Jour();
        return jour;
    }

    @Before
    public void initTest() {
        jour = createEntity(em);
    }

    @Test
    @Transactional
    public void createJour() throws Exception {
        int databaseSizeBeforeCreate = jourRepository.findAll().size();

        // Create the Jour
        restJourMockMvc.perform(post("/api/jours")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(jour)))
            .andExpect(status().isCreated());

        // Validate the Jour in the database
        List<Jour> jourList = jourRepository.findAll();
        assertThat(jourList).hasSize(databaseSizeBeforeCreate + 1);
        Jour testJour = jourList.get(jourList.size() - 1);
    }

    @Test
    @Transactional
    public void createJourWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = jourRepository.findAll().size();

        // Create the Jour with an existing ID
        jour.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restJourMockMvc.perform(post("/api/jours")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(jour)))
            .andExpect(status().isBadRequest());

        // Validate the Jour in the database
        List<Jour> jourList = jourRepository.findAll();
        assertThat(jourList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllJours() throws Exception {
        // Initialize the database
        jourRepository.saveAndFlush(jour);

        // Get all the jourList
        restJourMockMvc.perform(get("/api/jours?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(jour.getId().intValue())));
    }

    @Test
    @Transactional
    public void getJour() throws Exception {
        // Initialize the database
        jourRepository.saveAndFlush(jour);

        // Get the jour
        restJourMockMvc.perform(get("/api/jours/{id}", jour.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(jour.getId().intValue()));
    }

    @Test
    @Transactional
    public void getNonExistingJour() throws Exception {
        // Get the jour
        restJourMockMvc.perform(get("/api/jours/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateJour() throws Exception {
        // Initialize the database
        jourRepository.saveAndFlush(jour);
        int databaseSizeBeforeUpdate = jourRepository.findAll().size();

        // Update the jour
        Jour updatedJour = jourRepository.findOne(jour.getId());
        // Disconnect from session so that the updates on updatedJour are not directly saved in db
        em.detach(updatedJour);

        restJourMockMvc.perform(put("/api/jours")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedJour)))
            .andExpect(status().isOk());

        // Validate the Jour in the database
        List<Jour> jourList = jourRepository.findAll();
        assertThat(jourList).hasSize(databaseSizeBeforeUpdate);
        Jour testJour = jourList.get(jourList.size() - 1);
    }

    @Test
    @Transactional
    public void updateNonExistingJour() throws Exception {
        int databaseSizeBeforeUpdate = jourRepository.findAll().size();

        // Create the Jour

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restJourMockMvc.perform(put("/api/jours")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(jour)))
            .andExpect(status().isCreated());

        // Validate the Jour in the database
        List<Jour> jourList = jourRepository.findAll();
        assertThat(jourList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteJour() throws Exception {
        // Initialize the database
        jourRepository.saveAndFlush(jour);
        int databaseSizeBeforeDelete = jourRepository.findAll().size();

        // Get the jour
        restJourMockMvc.perform(delete("/api/jours/{id}", jour.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Jour> jourList = jourRepository.findAll();
        assertThat(jourList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Jour.class);
        Jour jour1 = new Jour();
        jour1.setId(1L);
        Jour jour2 = new Jour();
        jour2.setId(jour1.getId());
        assertThat(jour1).isEqualTo(jour2);
        jour2.setId(2L);
        assertThat(jour1).isNotEqualTo(jour2);
        jour1.setId(null);
        assertThat(jour1).isNotEqualTo(jour2);
    }
}
