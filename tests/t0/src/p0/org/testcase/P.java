package org.testcase;

import org.lib.L0;

public class P0 {

    private L0 l;

    public P0(L0 l) {
        this.l = l;
    }

    public void getL0() {
        return this.l;
    }

    public void f() {
        getL0().f();
    }

    public static void main() {
        new P0(new L0()).f();
    }
}
