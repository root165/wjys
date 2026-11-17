<script setup>
const metrics = [
  { label: '设备总数', value: '—', foot: 'M02 设备台账接入后展示' },
  { label: '待处理工单', value: '—', foot: 'M04 维修模块接入后展示' },
  { label: '今日巡检', value: '—', foot: 'M03 巡检模块接入后展示' },
  { label: '运行健康度', value: '—', foot: 'M07 统计模块接入后展示' },
]
</script>

<template>
  <div class="page-intro"><div><h2>早上好，{{ $route.meta.title === '工作台' ? '系统管理员' : '' }}</h2><p>这是维检云枢的基础工作台。业务模块正在按实施计划逐步接入。</p></div></div>
  <section class="metric-grid"><article v-for="metric in metrics" :key="metric.label" class="soft-card metric-card"><div class="metric-label">{{ metric.label }}</div><div class="metric-value">{{ metric.value }}</div><div class="metric-foot">{{ metric.foot }}</div></article></section>
  <section class="dashboard-grid"><article class="soft-card panel"><div class="panel-heading"><h3>今日工作流</h3><span>基础底座已就绪</span></div><div class="signal-list"><div class="signal-row"><div class="signal-badge">01</div><div class="signal-content"><strong>认证与权限</strong><span>JWT 登录、刷新、退出和权限快照</span></div><span class="signal-state">已完成</span></div><div class="signal-row"><div class="signal-badge">02</div><div class="signal-content"><strong>系统用户</strong><span>用户查询、创建与启停用接口</span></div><span class="signal-state">已完成</span></div><div class="signal-row"><div class="signal-badge">03</div><div class="signal-content"><strong>设备台账</strong><span>下一阶段：分类、设备、状态和二维码</span></div><span class="signal-state">待开始</span></div></div></article><article class="soft-card panel"><div class="panel-heading"><h3>运行提示</h3><span>P1</span></div><div class="empty-note">接入设备、巡检和工单数据后，这里会显示实时告警、任务提醒和关键指标。</div></article></section>
</template>
