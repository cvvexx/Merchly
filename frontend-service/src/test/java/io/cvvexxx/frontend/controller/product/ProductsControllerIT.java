package io.cvvexxx.frontend.controller.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
public class ProductsControllerIT {

    @Autowired
    MockMvc mockMvc;

    @Test
    void getNewProductPage_ReturnsProductPage() throws Exception {
        //given
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/catalogue/products/create")
                .with(user("root").roles("ADMIN"));
        //when
        mockMvc.perform(requestBuilder)
                //then
                .andExpectAll(
                        status().isOk(),
                        view().name("catalogue/products/create")
                );
    }

}
