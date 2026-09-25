package com.ryvenca.image;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ryvenca.auth.CurrentUser;
import com.ryvenca.image.ImageDtos.ImageUpload;
import com.ryvenca.user.UserService;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;
    private final UserService userService;

    public ImageController(ImageService imageService, UserService userService) {
        this.imageService = imageService;
        this.userService = userService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImageUpload upload(@AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file) {
        long userId = userService.require(CurrentUser.id(jwt)).getId();
        return imageService.upload(userId, file);
    }
}
