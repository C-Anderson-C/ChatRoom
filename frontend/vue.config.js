let proxyObj={};
proxyObj['/ws']={
  ws:true,
  target:"ws://localhost:8081"
};
proxyObj['/']={
  ws:false,
  target:'http://localhost:8081',
  changeOrigin: true,
  pathRewrite:{
    '^/':''
  }
}
module.exports={
  devServer:{
    host:'localhost',
    port:8083, // 改为 8083
    proxy:proxyObj
  }
}