package com.ashu.ride_sharing.utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Component
public class CloudinaryUtils {

    private final Cloudinary cloudinary;


    public CloudinaryUtils(Cloudinary cloudinary){
        this.cloudinary= cloudinary;
    }

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
        "image/jpeg",
        "image/png",
        "image/jpg",
        "image/webp"
    );

    public Map uploadImage(MultipartFile image) throws IOException{
        if (!ALLOWED_TYPES.contains(image.getContentType())) {
            throw new IllegalArgumentException();
        }
        return cloudinary.uploader().upload(
            image.getBytes(),ObjectUtils.asMap("resource_type","auto")
        );
    }

    public Map deleteImage(String publicId) throws IOException{
        return cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    public String extractPublicId(String studentImageURL){
        String [] parts = studentImageURL.split("/");
        String imageName = parts[parts.length-1];
        return imageName.substring(0,imageName.lastIndexOf('.'));
    }

}
