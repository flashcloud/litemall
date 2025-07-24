# CustomTabs 自定义选项卡组件

一个基于Vue的自定义选项卡组件，专为移动端设计，支持多种主题风格和徽章显示。

## 功能特性

- 🎨 **多主题支持** - 支持绿钻、情侣、听书等多种主题风格
- 🏷️ **徽章显示** - 支持在标签页显示徽章（如HOT、NEW等）
- 📱 **移动端优化** - 专为移动端设计，响应式布局
- 🎭 **插槽支持** - 灵活的内容插槽，可自定义标签页内容
- ⚡ **轻量高效** - 无额外依赖，性能优异

## 基础用法

```vue
<template>
  <custom-tabs v-model="activeTab" :tabs="tabs" @change="handleTabChange">
    <template v-slot="{ currentTab, activeTabData }">
      <div>当前标签页内容: {{ activeTabData.title }}</div>
    </template>
  </custom-tabs>
</template>

<script>
import CustomTabs from '@/components/CustomTabs/index.vue';

export default {
  components: { CustomTabs },
  data() {
    return {
      activeTab: 0,
      tabs: [
        { title: '标签一' },
        { title: '标签二' },
        { title: '标签三' }
      ]
    };
  },
  methods: {
    handleTabChange(tabIndex, tabData) {
      console.log('切换到:', tabIndex, tabData);
    }
  }
};
</script>
```

## 带徽章和副标题

```vue
<template>
  <custom-tabs v-model="activeTab" :tabs="tabs">
    <template v-slot="{ activeTabData }">
      <div>{{ activeTabData.description }}</div>
    </template>
  </custom-tabs>
</template>

<script>
export default {
  data() {
    return {
      activeTab: 0,
      tabs: [
        { 
          title: '超级会员', 
          subtitle: '限时福利',
          badge: 'HOT',
          description: '享受超级会员专属权益'
        },
        { 
          title: '普通会员', 
          subtitle: '基础服务',
          description: '基础会员服务'
        }
      ]
    };
  }
};
</script>
```

## 主题风格

组件支持多种预设主题，通过添加CSS类名来使用：

### 绿钻主题
```vue
<custom-tabs class="green-diamond-theme" :tabs="tabs" v-model="activeTab">
  <!-- 内容 -->
</custom-tabs>
```

### 情侣主题
```vue
<custom-tabs class="lover-theme" :tabs="tabs" v-model="activeTab">
  <!-- 内容 -->
</custom-tabs>
```

### 听书主题
```vue
<custom-tabs class="audio-theme" :tabs="tabs" v-model="activeTab">
  <!-- 内容 -->
</custom-tabs>
```

## API

### Props

| 参数 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| tabs | 标签页数据数组 | Array | [] |
| value/v-model | 当前激活的标签页索引 | Number | 0 |

### tabs 数组项属性

| 参数 | 说明 | 类型 | 是否必填 |
|------|------|------|----------|
| title | 标签页标题 | String | 是 |
| subtitle | 标签页副标题 | String | 否 |
| badge | 徽章文本 | String | 否 |
| description | 描述信息（传递给插槽） | String | 否 |

### Events

| 事件名 | 说明 | 回调参数 |
|--------|------|----------|
| change | 标签页切换时触发 | (tabIndex: number, tabData: object) |
| input | 当前标签页索引变化时触发 | (tabIndex: number) |

### Slots

| 名称 | 说明 | 参数 |
|------|------|------|
| default | 标签页内容区域 | { currentTab: number, activeTabData: object } |

## 样式定制

### CSS 变量

组件使用了以下CSS变量，可以通过覆盖这些变量来自定义样式：

```scss
.custom-tabs {
  --primary-color: #667eea;
  --secondary-color: #764ba2;
  --text-color: #333;
  --border-radius: 12px;
  --padding: 20px;
}
```

### 自定义主题

你可以创建自己的主题样式：

```scss
.custom-tabs.my-theme {
  .tabs-header {
    background: linear-gradient(135deg, #your-color-1 0%, #your-color-2 100%);
  }
}
```

## 在会员方案中的实际应用

这个组件特别适用于会员方案选择页面，如：

```vue
<template>
  <custom-tabs 
    v-model="memberType" 
    :tabs="memberTabs" 
    class="green-diamond-theme"
    @change="onMemberChange"
  >
    <template v-slot="{ activeTabData }">
      <div class="member-content">
        <div class="benefits">
          <!-- 会员权益展示 -->
        </div>
        <div class="pricing">
          <!-- 价格信息 -->
        </div>
      </div>
    </template>
  </custom-tabs>
</template>
```

## 注意事项

1. 确保项目中已安装并配置了Vant UI组件库（用于图标等）
2. 组件使用了SCSS，确保项目支持SCSS编译
3. 响应式断点为768px，可根据需要调整
4. 建议标签页数量不超过4个，以保证移动端体验

## 兼容性

- Vue 2.x
- 现代浏览器 (Chrome, Firefox, Safari, Edge)
- 移动端浏览器
- 支持iOS Safari和Android Chrome
