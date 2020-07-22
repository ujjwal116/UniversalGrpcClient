package com.ugc;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Test {
    public static void main(String... args) {
        ScriptEngine js = new ScriptEngineManager().getEngineByName("Nashorn");
        try {
            js.eval("print('" + Test.class.getSimpleName() + "')");
        } catch (ScriptException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
