let proxyObj = {};

// WebSocket代理配置
proxyObj['/ws'] = {
  ws: true,
  target: "ws://localhost:8081",
  changeOrigin: true
};

// HTTP请求代理配置
proxyObj['/'] = {
  ws: false,
  target: 'http://localhost:8081',
  changeOrigin: true,
  pathRewrite: {
    '^/': ''
  }
};

module.exports = {
  devServer: {
    host: 'localhost',
    port: 8082,
    proxy: proxyObj,
    // 移除setupMiddlewares和不兼容的选项
    compress: true
    // 移除maxHttpHeaderSize - 这可能是一个不支持的选项
  },
  // 保留其他配置
  configureWebpack: {
    optimization: {
      splitChunks: {
        chunks: 'all',
        maxInitialRequests: Infinity,
        minSize: 20000,
        maxSize: 250000,
        cacheGroups: {
          vendor: {
            test: /[\\/]node_modules[\\/]/,
            name(module) {
              const packageName = module.context.match(/[\\/]node_modules[\\/](.*?)([\\/]|$)/)[1];
              return `npm.${packageName.replace('@', '')}`;
            }
          }
        }
      }
    },
    performance: {
      hints: 'warning',
      maxEntrypointSize: 512000,
      maxAssetSize: 512000
    }
  },
  productionSourceMap: false
};