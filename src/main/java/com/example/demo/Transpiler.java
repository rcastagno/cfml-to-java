package com.example.demo;

import cfml.parsing.CFMLParser;
import cfml.parsing.cfscript.*;
import cfml.parsing.cfscript.script.CFScriptStatement;
import net.htmlparser.jericho.Element;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Transpiler {
    public static void main(String args[]) throws IOException {
        CFMLParser fCfmlParser;
        System.out.println(Paths.get(".").toAbsolutePath().toString());
        final String sourceUrlFile = "file:src/main/resources/test3.cfm";
        final String filename = Paths.get(sourceUrlFile).getFileName().toString();
        final String basename = FilenameUtils.getBaseName(filename);
        final String fileext = FilenameUtils.getExtension(filename);

        fCfmlParser = new CFMLParser();
        try {
            fCfmlParser.addCFMLSource(new URL(sourceUrlFile));

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        fCfmlParser.parse();

        ArrayList<String> output = new ArrayList<>();

        for (Element element : fCfmlParser.getAllTags()) {
            String elementName = element.getName();
            switch(elementName) {
                case "cfscript":
                    CFScriptStatement cfScriptStatement = fCfmlParser.parseScript(element.getSource().toString());
                    for (CFScriptStatement scriptStatement : cfScriptStatement.decomposeScript()) {
                        for (CFExpression cfExpression : scriptStatement.decomposeExpression()) {

                            String expressionClassName = cfExpression.getClass().getName();

                            switch (expressionClassName) {
                                case "cfml.parsing.cfscript.CFVarDeclExpression":
                                    CFExpression var = ((CFVarDeclExpression) cfExpression).getVar();
                                    String varClass = var.getClass().getName();
                                    switch (varClass) {
                                        case "cfml.parsing.cfscript.CFIdentifier":
                                            CFExpression init = ((CFVarDeclExpression) cfExpression).getInit();
                                            String initClass = init.getClass().getName();
                                            String name = ((CFIdentifier) var).getName();

                                            String src = "";
                                            switch (initClass) {
                                                case "cfml.parsing.cfscript.CFStringExpression":
                                                    String initValue = ((CFLiteral) ((CFStringExpression) init).getSubExpressions().get(0)).getVal();
                                                    src = "String " + name + " = \"" + initValue + "\";";
                                                    output.add(src);
                                                    break;
                                                case "cfml.parsing.cfscript.CFBinaryExpression":


                                                    String expression = getResultOfBinaryExpression((CFBinaryExpression) init);

                                                    src = "String " + name + " = " + expression + ";";
                                                    output.add(src);
                                                    break;
                                                default:
                                                    System.out.println("Identifier class not managed: " + initClass);
                                                    break;
                                            }

                                            break;
                                        default:
                                            System.out.println("Variable declaration not managed: " + varClass);
                                    }
                                    break;
                                case "cfml.parsing.cfscript.CFFunctionExpression":
                                    String functionName = ((CFFunctionExpression) cfExpression).getName();
                                    switch (functionName) {
                                        case "writeOutput":
                                            ArrayList<String> src = new ArrayList<>();
                                            src.add("System.out.println(");

                                            ArrayList<String> myargs = new ArrayList<>();
                                            for (CFExpression arg : ((CFFunctionExpression) cfExpression).getArgs()) {
                                                myargs.add(arg.toString());
                                            }
                                            src.add(String.join(",", myargs));
                                            src.add(");");

                                            output.add(String.join("", src));

                                            break;
                                        default:
                                            System.out.println("Function expression not managed: " + functionName);
                                            break;
                                    }
                                    break;
                                default:
                                    System.out.println("Expression not managed: " + expressionClassName);
                                    break;
                            }
                        }
                    }
                    break;
                case "cfquery":
                    String queryName = element.getAttributes().getValue("name");

                    output.add("String queryName = \"" + queryName + "\";");

                    // the full sql with all cfqueryparams
                    String fullSql = element.getContent().getSource().getParseText().toString();
                    fullSql = fullSql.replace(element.getStartTag().toString(), "");
                    fullSql = fullSql.replace(element.getEndTag().toString(), "");

                    output.add("Map<String, String> queryParametersValues = new HashMap<>();");
                    output.add("Map<String, String> queryParametersTypes = new HashMap<>();");

                    // should iterate all sub-elements
                    for (Element childElement : element.getChildElements()) {
                        String cfsqltype = childElement.getAttributeValue("cfsqltype");
                        String value = childElement.getAttributeValue("value").replace("#", "");
                        String paramName = value.replace(".", "_");
                        fullSql = fullSql.replace(childElement.getStartTag().toString(), ":" + paramName);
                        output.add("queryParametersValues.put(\"" + paramName + "\", \"" + value + "\");");
                        output.add("queryParametersTypes.put(\"" + paramName + "\", \"" + cfsqltype + "\");");
                    }

                    output.add("String fullSql = \"" + fullSql + "\";");


                    //String sql = element.getContent().getTextExtractor().toString();

                    //elements.get(0).getStartTag().getAttributes();


                    break;
                case "cfqueryparam":
                    break;
                default:
                    System.out.println("Element not managed: " + elementName);
                    break;
            }
        }


        String generateJavaSource = String.join("\n", output);

        System.out.println(generateJavaSource);

    }

    private static String getResultOfBinaryExpression(CFBinaryExpression init) {
        String expression = "";
        if(init.getLeft() instanceof CFBinaryExpression) {
            String ope = init.getOperatorImage();
            String left = getResultOfBinaryExpression((CFBinaryExpression) init.getLeft());

            String right = init.getRight().toString();
            if(init.getRight() instanceof CFStringExpression) {
                right = ((CFLiteral) ((CFStringExpression) init.getRight()).getSubExpressions().get(0)).getVal();
                right = "\"" + right + "\"";
            }

            expression = left + ope + right;
        }
        else {
            String ope = init.getOperatorImage();
            String left = init.getLeft().toString();
            String right = init.getRight().toString();
            expression = left + ope + right;
        }
        return expression;
    }
}
