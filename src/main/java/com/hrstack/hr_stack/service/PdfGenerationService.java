package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.util.PdfGeneratorUtil;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class PdfGenerationService {

    private final TemplateEngine templateEngine;

    public PdfGenerationService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generatePdf(
            String templateName,
            Map<String, Object> data) {

        Context context = new Context();
        context.setVariables(data);

        String htmlContent =
                templateEngine.process(templateName, context);

        return PdfGeneratorUtil.generatePdf(htmlContent);
    }
}