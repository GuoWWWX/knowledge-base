package com.knowledge.pattern.structural;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 结构型模式实战：组合模式 (Composite Pattern)
 * 1. 核心思想：将对象组合成树形结构以表示“部分-整体”的层次结构，使得客户端对单个对象和组合对象的使用具有一致性
 * 2. 经典对标：文件系统目录树 (Folder / File)、UI 控件树 (ViewGroup / View)、企业组织架构 (部门 / 员工)
 * 3. 业务实战：企业级 RBAC 动态权限菜单树构建与权限鉴权剪枝过滤 (Permission Pruning)
 */
public class CompositeMenuTreeDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. 使用组合模式构建多级权限菜单树 ==========");
        MenuComponent root = buildEnterpriseMenuTree();

        System.out.println("\n========== 2. 递归打印完整菜单结构 (部分与整体一致处理) ==========");
        root.print("");

        System.out.println("\n========== 3. 角色动态鉴权剪枝 (仅保留拥有权限的菜单节点) ==========");
        // 假设当前运维人员仅拥有: [sys:user:list, sys:log:audit, order:list] 权限
        Set<String> userPermissions = Set.of("sys:user:list", "sys:log:audit", "order:list");
        MenuComponent prunedMenu = root.filterByPermissions(userPermissions);

        System.out.println("剪枝过滤后的用户专属可见菜单树 (JSON 格式化):");
        System.out.println(JSON.toJSONString(prunedMenu, JSONWriter.Feature.PrettyFormat));
    }

    private static MenuComponent buildEnterpriseMenuTree() {
        MenuComposite root = new MenuComposite("root", "主控制台", null);

        // 系统管理模块
        MenuComposite systemMgmt = new MenuComposite("sys", "系统管理", "sys:view");
        systemMgmt.add(new MenuItem("sys:user", "用户管理", "sys:user:list", "/system/users"));
        systemMgmt.add(new MenuItem("sys:role", "角色权限", "sys:role:list", "/system/roles"));
        systemMgmt.add(new MenuItem("sys:log", "审计日志", "sys:log:audit", "/system/logs"));

        // 交易中心模块
        MenuComposite tradeMgmt = new MenuComposite("trade", "交易履约中心", "trade:view");
        tradeMgmt.add(new MenuItem("trade:order", "订单列表", "order:list", "/trade/orders"));
        tradeMgmt.add(new MenuItem("trade:refund", "售后退款", "refund:audit", "/trade/refunds"));

        root.add(systemMgmt);
        root.add(tradeMgmt);
        return root;
    }

    // 抽象组件
    public static abstract class MenuComponent {
        protected String id;
        protected String name;
        protected String permission;

        public MenuComponent(String id, String name, String permission) {
            this.id = id;
            this.name = name;
            this.permission = permission;
        }

        public abstract void print(String prefix);
        public abstract MenuComponent filterByPermissions(Set<String> permissions);

        public String getId() { return id; }
        public String getName() { return name; }
        public String getPermission() { return permission; }
    }

    // 叶子节点 (具体功能菜单项)
    public static class MenuItem extends MenuComponent {
        private final String routePath;

        public MenuItem(String id, String name, String permission, String routePath) {
            super(id, name, permission);
            this.routePath = routePath;
        }

        @Override
        public void print(String prefix) {
            System.out.printf("%s└── [菜单项] %s (路径: %s, 权限: %s)%n", prefix, name, routePath, permission);
        }

        @Override
        public MenuComponent filterByPermissions(Set<String> permissions) {
            if (permission == null || permissions.contains(permission)) {
                return new MenuItem(id, name, permission, routePath);
            }
            return null; // 无权限，剪枝淘汰
        }

        public String getRoutePath() { return routePath; }
    }

    // 复合容器节点 (目录/父菜单)
    public static class MenuComposite extends MenuComponent {
        private final List<MenuComponent> children = new ArrayList<>();

        public MenuComposite(String id, String name, String permission) {
            super(id, name, permission);
        }

        public void add(MenuComponent component) {
            children.add(component);
        }

        @Override
        public void print(String prefix) {
            System.out.printf("%s├── [目录节点] %s%n", prefix, name);
            for (MenuComponent child : children) {
                child.print(prefix + "│   ");
            }
        }

        @Override
        public MenuComponent filterByPermissions(Set<String> permissions) {
            MenuComposite filteredComposite = new MenuComposite(id, name, permission);
            for (MenuComponent child : children) {
                MenuComponent filteredChild = child.filterByPermissions(permissions);
                if (filteredChild != null) {
                    filteredComposite.add(filteredChild);
                }
            }
            // 如果子节点全被剪枝淘汰且自身也无权，则该目录剪除
            if (filteredComposite.children.isEmpty() && !"root".equals(id)) {
                return null;
            }
            return filteredComposite;
        }

        public List<MenuComponent> getChildren() {
            return children;
        }
    }
}
