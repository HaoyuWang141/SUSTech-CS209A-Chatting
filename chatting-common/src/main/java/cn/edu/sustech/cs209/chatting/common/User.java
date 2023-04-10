package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;
import java.net.Socket;
import java.util.Objects;

public record User(String name, String pwd) implements Serializable {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(name, user.name);
    }

    @Override
    public String toString() {
        return "User{" +
            "name='" + name + '\'' +
            ", pwd='" + pwd + '\'' +
            '}';
    }
}