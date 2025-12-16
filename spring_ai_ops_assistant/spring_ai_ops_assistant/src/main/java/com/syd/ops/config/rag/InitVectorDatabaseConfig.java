package com.syd.ops.config.rag;

import cn.hutool.crypto.SecureUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.List;

/**
 * 将向量加载到向量数据库redis中
 */
@Component
public class InitVectorDatabaseConfig {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("classpath:error_code.txt")
    private Resource errorCodeFile;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @PostConstruct
    public void init() {
        // 1.读取文件
        TextReader textReader = new TextReader(errorCodeFile);
        textReader.setCharset(Charset.defaultCharset());

        // 2.文件转为向量(开启分词)
        List<Document> list = new TokenTextSplitter().transform(textReader.read());


        //3.去重
        /*String sourceMetadata =  (String)textReader.getCustomMetadata().get("source");
        String textHash = SecureUtil.md5(sourceMetadata);
        String redisKey = "vector-xxx:" + textHash;

        Boolean retFlag = redisTemplate.opsForValue().setIfAbsent(redisKey,"1");
        if(retFlag) {
            // 键不存在，插入
            vectorStore.add(list);
            logger.info("向量初始化数据加载完成");
        } else {
            // 已存在
            logger.info("向量初始化数据已经加载过，无需重复操作");
        }*/

        // 4. 加载
        vectorStore.add(list);

    }

}
