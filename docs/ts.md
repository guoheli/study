```


ffmpeg:  https://www.kancloud.cn/zhenhuamcu/ffmpeg/1034137

ts 转 mp4

ffmpeg -i input.ts -c:v libx264 -c:a aac input.mp4




ffmpeg -y -i input2.ts -c:v libx264 -c:a copy -bsf:a aac_adtstoasc output2.mp4




ffmpeg -i "concat:A1C-1640053898251.ts|A1C-1640053900248.ts" -acodec copy -vcodec copy -absf aac_adtstoasc output.mp4



正常转： ffmpeg  -i "http://xxxxxx/video/movie.m3u8" -vcodec copy -acodec copy -absf aac_adtstoasc  output.mp4



videofile = 'images/camera_2gFnDh5i_2020092314_A1C-1600843171531.ts'
```