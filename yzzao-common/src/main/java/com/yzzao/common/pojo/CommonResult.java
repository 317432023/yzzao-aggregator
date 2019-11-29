/*     */ package com.yzzao.common.pojo;
/*     */ 
/*     */ import com.fasterxml.jackson.databind.JsonNode;
/*     */ import com.fasterxml.jackson.databind.ObjectMapper;
/*     */ import com.fasterxml.jackson.databind.type.TypeFactory;
/*     */ import java.util.List;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class CommonResult
/*     */ {
/*  14 */   private static final ObjectMapper MAPPER = new ObjectMapper();
/*     */   
/*     */ 
/*     */   private Integer status;
/*     */   
/*     */   private String msg;
/*     */   
/*     */   private Object data;
/*     */   
/*     */ 
/*     */   public static CommonResult build(Integer status, String msg, Object data)
/*     */   {
/*  26 */     return new CommonResult(status, msg, data);
/*     */   }
/*     */   
/*     */   public static CommonResult ok(Object data) {
/*  30 */     return new CommonResult(data);
/*     */   }
/*     */   
/*     */   public static CommonResult ok() {
/*  34 */     return new CommonResult(null);
/*     */   }
/*     */   
/*     */ 
/*     */   public CommonResult() {}
/*     */   
/*     */   public static CommonResult build(Integer status, String msg)
/*     */   {
/*  42 */     return new CommonResult(status, msg, null);
/*     */   }
/*     */   
/*     */   public CommonResult(Integer status, String msg, Object data) {
/*  46 */     this.status = status;
/*  47 */     this.msg = msg;
/*  48 */     this.data = data;
/*     */   }
/*     */   
/*     */   public CommonResult(Object data) {
/*  52 */     this.status = Integer.valueOf(200);
/*  53 */     this.msg = "OK";
/*  54 */     this.data = data;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public Integer getStatus()
/*     */   {
/*  62 */     return this.status;
/*     */   }
/*     */   
/*     */   public void setStatus(Integer status) {
/*  66 */     this.status = status;
/*     */   }
/*     */   
/*     */   public String getMsg() {
/*  70 */     return this.msg;
/*     */   }
/*     */   
/*     */   public void setMsg(String msg) {
/*  74 */     this.msg = msg;
/*     */   }
/*     */   
/*     */   public Object getData() {
/*  78 */     return this.data;
/*     */   }
/*     */   
/*     */   public void setData(Object data) {
/*  82 */     this.data = data;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static CommonResult formatToPojo(String jsonData, Class<?> clazz)
/*     */   {
/*     */     try
/*     */     {
/*  94 */       if (clazz == null) {
/*  95 */         return (CommonResult)MAPPER.readValue(jsonData, CommonResult.class);
/*     */       }
/*  97 */       JsonNode jsonNode = MAPPER.readTree(jsonData);
/*  98 */       JsonNode data = jsonNode.get("data");
/*  99 */       Object obj = null;
/* 100 */       if (clazz != null) {
/* 101 */         if (data.isObject()) {
/* 102 */           obj = MAPPER.readValue(data.traverse(), clazz);
/* 103 */         } else if (data.isTextual()) {
/* 104 */           obj = MAPPER.readValue(data.asText(), clazz);
/*     */         }
/*     */       }
/* 107 */       return build(Integer.valueOf(jsonNode.get("status").intValue()), jsonNode.get("msg").asText(), obj);
/*     */     } catch (Exception e) {}
/* 109 */     return null;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static CommonResult format(String json)
/*     */   {
/*     */     try
/*     */     {
/* 121 */       return (CommonResult)MAPPER.readValue(json, CommonResult.class);
/*     */     } catch (Exception e) {
/* 123 */       e.printStackTrace();
/*     */     }
/* 125 */     return null;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static CommonResult formatToList(String jsonData, Class<?> clazz)
/*     */   {
/*     */     try
/*     */     {
/* 137 */       JsonNode jsonNode = MAPPER.readTree(jsonData);
/* 138 */       JsonNode data = jsonNode.get("data");
/* 139 */       Object obj = null;
/* 140 */       if ((data.isArray()) && (data.size() > 0)) {
/* 141 */         obj = MAPPER.readValue(data.traverse(), 
/* 142 */           MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
/*     */       }
/* 144 */       return build(Integer.valueOf(jsonNode.get("status").intValue()), jsonNode.get("msg").asText(), obj);
/*     */     } catch (Exception e) {}
/* 146 */     return null;
/*     */   }
/*     */ }


/* Location:              E:\Project\佰源MES\安装包压缩包\mes-client-install\lib\yzzao-common-0.0.1-SNAPSHOT.jar!\com\yzzao\common\pojo\CommonResult.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */