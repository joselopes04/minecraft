package jogo.gameobject.character;

import jogo.gameobject.GameObject;

public abstract class Character extends GameObject {

    protected Character(String name) {
        super(name);
    }

    // Example state hooks students can extend
    private int health = 100;

    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
}
