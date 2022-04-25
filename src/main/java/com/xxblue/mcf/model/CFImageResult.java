package com.xxblue.mcf.model;

import lombok.Data;

import java.util.List;

@Data
public class CFImageResult {

    boolean success;

    String uid;

    List<String> imageUrl;

}
