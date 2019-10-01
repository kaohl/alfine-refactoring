package org.lib;

public class L0 {

    public void f() {
        int x = g();

        /* The purpose of this print is to make sure framework does not crash
           when it encounters method invocations for which the binding can not
           be resolved. */
        System.out.println("g() = " + x);
    }

    private int g() {
        int sum = 0;
        for (int i = 0; i < 10; ++i) {
            sum += i;
        }
        return sum;
    }

    public int h() {
        for (int i = g(); i > 10; --i) {
            System.out.println("" + i);
        }
        return 5;
    }

    public int e() {
        int i = 0;
        for (i = g(); i > 0; --i) {
            System.out.println("" + g());
        }
        return 0;
    }

    public L0 newL0() {
        return new L0();
    }

}
