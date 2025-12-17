package ink.icoding.smartmybatis.example.entity;

import ink.icoding.smartmybatis.entity.po.PO;
import ink.icoding.smartmybatis.entity.po.enums.ID;
import ink.icoding.smartmybatis.entity.po.enums.TableField;
import ink.icoding.smartmybatis.example.enums.Sex;

import java.util.List;

/**
 * Student 实体类, 对应数据库中的 student 表
 * @author gsk
 */
public class Student extends PO {

    @ID
    private int id;

    private String name;

    private int age;

    private Sex sex;

    @TableField(json = true, description = "爱好列表")
    private List<String> hobbies;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public List<String> getHobbies() {
        return hobbies;
    }

    public void setHobbies(List<String> hobbies) {
        this.hobbies = hobbies;
    }
}
