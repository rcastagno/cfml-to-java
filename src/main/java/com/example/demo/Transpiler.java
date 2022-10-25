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

public class Transpiler {
    public static void main(String args[]) throws IOException {
        CFMLParser fCfmlParser;
        System.out.println(Paths.get(".").toAbsolutePath().toString());
        final String sourceUrlFile = "file:src/main/resources/test2.cfm";
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
            switch(element.getName()) {
                case "cfscript":
                    CFScriptStatement cfScriptStatement = fCfmlParser.parseScript(element.getSource().toString());
                    for (CFScriptStatement scriptStatement : cfScriptStatement.decomposeScript()) {
                        for (CFExpression cfExpression : scriptStatement.decomposeExpression()) {

                            String cl = cfExpression.getClass().getName();

                            switch (cl) {
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
                                                    System.out.println(initClass);
                                                    break;
                                            }

                                            break;
                                        default:
                                            System.out.println(varClass);
                                    }
                                    break;
                                case "cfml.parsing.cfscript.CFFunctionExpression":
                                    String fn = ((CFFunctionExpression) cfExpression).getName();
                                    switch (fn) {
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
                                            System.out.println(fn);
                                            break;
                                    }
                                    break;
                                default:
                                    System.out.println(cl);
                                    break;
                            }
                        }
                    }
                    break;
                default:
                    System.out.println(element.getName());
                    break;
            }
        }
        System.out.println(String.join("\n", output));

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
