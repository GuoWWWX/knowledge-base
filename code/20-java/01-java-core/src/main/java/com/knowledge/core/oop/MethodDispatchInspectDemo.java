package com.knowledge.core.oop;

/**
 * JVM 方法分派深度解析：
 * 1. 静态分派 (Static Dispatch): 重载 (Overload)，编译期依据声明的静态类型确定目标方法
 * 2. 动态分派 (Dynamic Dispatch): 重写 (Override)，运行期依据实际对象的类型确定，执行 invokevirtual
 * 3. 虚方法表 (Virtual Method Table / vtable) 寻址与多态实现机制
 */
public class MethodDispatchInspectDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. 静态分派（方法重载 Overload）实证 ==========");
        Human man = new Man();
        Human woman = new Woman();

        Dispatcher dispatcher = new Dispatcher();
        // 编译期依据静态类型（Human），即使运行时实际是 Man/Woman，调用的依然是 sayHello(Human)
        System.out.print("dispatcher.sayHello(man)   -> ");
        dispatcher.sayHello(man);
        System.out.print("dispatcher.sayHello(woman) -> ");
        dispatcher.sayHello(woman);

        System.out.println("\n========== 2. 动态分派（方法重写 Override）实证 ==========");
        // 运行期通过 invokevirtual 字节码指令，根据栈顶对象的实际类型指针查找虚方法表
        System.out.print("man.executeAction()   -> ");
        man.executeAction();
        System.out.print("woman.executeAction() -> ");
        woman.executeAction();

        System.out.println("\n========== 3. 动态双分派模拟（Visitor 设计模式）==========");
        man.accept(dispatcher);
        woman.accept(dispatcher);
    }

    // 静态基类与派生类
    abstract static class Human {
        public abstract void executeAction();
        public abstract void accept(Dispatcher dispatcher);
    }

    static class Man extends Human {
        @Override
        public void executeAction() {
            System.out.println("【Man 动态分派执行】: 逻辑思维与决断");
        }

        @Override
        public void accept(Dispatcher dispatcher) {
            // 通过 this 明确传递当前实际类型 (Man)，完成二次分派
            dispatcher.sayHello(this);
        }
    }

    static class Woman extends Human {
        @Override
        public void executeAction() {
            System.out.println("【Woman 动态分派执行】: 细腻感知与共情");
        }

        @Override
        public void accept(Dispatcher dispatcher) {
            // 通过 this 明确传递当前实际类型 (Woman)，完成二次分派
            dispatcher.sayHello(this);
        }
    }

    // 重载方法分派器
    static class Dispatcher {
        public void sayHello(Human human) {
            System.out.println("Hello, Human! (静态类型匹配)");
        }

        public void sayHello(Man man) {
            System.out.println("Hello, Gentleman! (二次分派实际类型)");
        }

        public void sayHello(Woman woman) {
            System.out.println("Hello, Lady! (二次分派实际类型)");
        }
    }
}
