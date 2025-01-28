package io.ballerina.flowmodelgenerator.extension.typesmanager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.flowmodelgenerator.extension.AbstractLSTest;
import io.ballerina.flowmodelgenerator.extension.request.GetTypeRequest;
import io.ballerina.tools.text.LinePosition;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test cases for retrieving types for GraphQL.
 *
 * @since 2.0.0
 */
public class GetGraphqlTypeTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        GetTypeRequest request = new GetTypeRequest(
                sourceDir.resolve(testConfig.filePath()).toAbsolutePath().toString(),
                testConfig.position());
        JsonObject response = getResponse(request);
        JsonElement typeResponse = response.get("type");
        JsonElement refsResponse = response.get("refs");
        if (!typeResponse.equals(testConfig.type()) || !refsResponse.equals(testConfig.refs())) {
            TestConfig updateConfig = new TestConfig(testConfig.filePath(), testConfig.position(),
                    testConfig.description(), typeResponse, refsResponse);
//            updateConfig(configJsonPath, updateConfig);
            compareJsonElements(typeResponse, testConfig.type());
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @DataProvider(name = "data-provider")
    @Override
    protected Object[] getConfigsList() {
        return new Object[][]{
//                {Path.of("get_service_class1.json")},
//                {Path.of("get_service_class2.json")},
                {Path.of("get_service_class3.json")},
        };
    }

    @Override
    protected String getResourceDir() {
        return "types_manager";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return GetGraphqlTypeTest.class;
    }

    @Override
    protected String getApiName() {
        return "getGraphqlType";
    }

    @Override
    protected String getServiceName() {
        return "typesManager";
    }

    private record TestConfig(String filePath,
                              LinePosition position,
                              String description,
                              JsonElement type,
                              JsonElement refs) {
    }
}
