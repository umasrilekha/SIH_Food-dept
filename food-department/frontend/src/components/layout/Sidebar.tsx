import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Inbox,
  FileText,
  FolderKanban,
  Network,
  ShieldCheck,
  ClipboardList,
  Bell,
  Activity,
  UserCheck,
  RefreshCw
} from 'lucide-react';

export const Sidebar: React.FC = () => {
  const navItems = [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/requests', label: 'Incoming Requests', icon: Inbox },
    { to: '/ration-records', label: 'Ration Records', icon: FileText },
    { to: '/applications', label: 'Applications Search', icon: FolderKanban },
    { to: '/integration', label: 'Interoperability Monitor', icon: Network },
    { to: '/integration/retries', label: 'Failed & Retries', icon: RefreshCw },
    { to: '/consent', label: 'Consent & Compliance', icon: ShieldCheck },
    { to: '/audit-logs', label: 'Audit Logs', icon: ClipboardList },
    { to: '/notifications', label: 'Notifications Feed', icon: Bell },
    { to: '/system-health', label: 'System Diagnostics', icon: Activity },
    { to: '/profile', label: 'Officer Profile', icon: UserCheck },
  ];

  return (
    <div className="w-52 bg-white text-slate-700 border-r border-slate-300 shrink-0 hidden lg:block">
      <div className="px-3 py-2 text-[10px] font-bold uppercase tracking-wider text-slate-500 border-b border-slate-200 bg-slate-100">
        Officer Navigation
      </div>
      <nav className="py-1 space-y-0.5 text-xs">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center px-3 py-2 transition ${
                  isActive
                    ? 'bg-blue-50 border-l-4 border-blue-900 text-blue-950 font-bold'
                    : 'text-slate-700 hover:bg-slate-50 font-medium'
                }`
              }
            >
              <Icon className="w-3.5 h-3.5 mr-2 shrink-0 text-slate-500" />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>
    </div>
  );
};
