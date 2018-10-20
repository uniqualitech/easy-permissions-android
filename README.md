# easy-permissions-android
we can use this library when run time permission is needed for take photo from gallery and camera


*For Camera*
new GalleryCameraPhotoHandler.Builder().setContext(getActivity())
                    .setPhotoHandler(GalleryCameraPhotoHandler.PhotoHandler.CAMERA)
                    .setPhotoCallback(new GalleryCameraPhotoHandler.PickPhotoCallback() {
                        @Override
                        public void photoPath(String path, GalleryCameraPhotoHandler.PhotoHandler photoHandler) {
                            Log.e("path","path "+path);
                            if (path != null)
                                strImagePath = new ImageCompressor(getActivity()).compressImage(path);
                            imgProfile.setImageBitmap(ImageUtils.getInstant().getCompressedBitmap(getActivity(),path));
                        }
                    })
                    .build();
                    
                    
*For Gallery*
new GalleryCameraPhotoHandler.Builder().setContext(getActivity())
                    .setPhotoHandler(GalleryCameraPhotoHandler.PhotoHandler.GALLERY)
                    .setPhotoCallback(new GalleryCameraPhotoHandler.PickPhotoCallback() {
                        @Override
                        public void photoPath(String path, GalleryCameraPhotoHandler.PhotoHandler photoHandler) {
                            Log.e("path","path "+path);
                            if (path != null)
                                strImagePath = new ImageCompressor(getActivity()).compressImage(path);
                            imgProfile.setImageBitmap(ImageUtils.getInstant().getCompressedBitmap(getActivity(),path));
                        }
                    })
                    .build();


*For Other Permission*
new PermissionHandler.Builder().setContext(getWeakActivity())
                .setAllPermission(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
                .setPermissionCallback(new PermissionInterface() {
                    @Override
                    public void permissionGranted() {
                        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(galleryIntent, PICK_GALLERY);
                    }

                    @Override
                    public void permissionDenied() {
                       
                    }
                }).build();
